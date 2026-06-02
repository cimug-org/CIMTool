<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 2026 UCAIug

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:a="http://langdale.com.au/2005/Message#"
    xmlns:sawsdl="http://www.w3.org/ns/sawsdl"
    xmlns:map="http://www.w3.org/2005/xpath-functions/map"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:cimtool="http://cimtool.ucaiug.io/functions"
    xmlns="http://langdale.com.au/2009/Indent">

    <xsl:output indent="yes" method="xml" encoding="UTF-8" />
    <xsl:param name="version"/>
    <xsl:param name="baseURI"/>
    <xsl:param name="envelope">Profile</xsl:param>
    <xsl:param name="package">io.ucaiug.cimtool.generated</xsl:param>
    <xsl:param name="mridType">String</xsl:param>

    <!--
        Function: cimtool:has-mrid-ancestor
        Purpose:  Returns true if the given element (a:Root or a:ComplexType) is
                  in the IdentifiedObject hierarchy — i.e. if the element itself or
                  any ancestor in its a:SuperType chain directly declares an
                  attribute named 'mRID'.

                  Non-IdentifiedObject classes (e.g. CurveData, TapChangerTablePoint,
                  Quality61850, ConductingEquipment in StateVariables) have no mRID
                  ancestor and must use a surrogate UUID 'id' as their JPA @Id rather
                  than the natural 'mRID' string key used by IdentifiedObject subclasses.

                  This is the definitive programmatic test for membership in the
                  IdentifiedObject hierarchy and mirrors the identical function in the
                  companion sql-rdfs-ansi92.xsl and csharp-ef-rdfs.xsl builders.
        Parameters:
            $element — The a:Root or a:ComplexType element to test
        Returns:
            xs:boolean — true if mRID is present at this class or any ancestor
    -->
    <xsl:function name="cimtool:has-mrid-ancestor" as="xs:boolean">
        <xsl:param name="element" as="element()"/>
        <xsl:sequence select="
            if ($element/a:Simple[@name='mRID']) then
                true()
            else if ($element/a:SuperType) then
                let $superBaseClass := string($element/a:SuperType[1]/@baseClass),
                    $parent := (
                        root($element)//a:ComplexType[@baseClass = $superBaseClass]
                        | root($element)//a:Root[@baseClass = $superBaseClass]
                    )[1]
                return
                    if ($parent) then cimtool:has-mrid-ancestor($parent)
                    else false()
            else
                false()
        "/>
    </xsl:function>
    
    <!--
        Function: cimtool:javaType
        Maps an XSD type string to the corresponding Java type name used in
        generated entity classes.  All numeric CIM types map to Double to
        preserve precision across the full range of CIM measurement values.
        NOTE: dateTime maps to LocalDateTime (no timezone). If timezone-aware
        storage is required use the dateTimeStamp xstype which maps to
        OffsetDateTime, equivalent to TIMESTAMP WITH TIME ZONE in SQL.
        Parameters:
            $xstype — the XSD type string (e.g. 'string', 'float', 'boolean')
        Returns:
            xs:string — the Java type name (e.g. 'String', 'Double', 'Boolean')
    -->
    <xsl:function name="cimtool:javaType" as="xs:string">
        <xsl:param name="xstype" as="xs:string"/>
        <xsl:sequence select="
            if      ($xstype = 'string')                then 'String'
            else if ($xstype = ('integer', 'int'))      then 'Integer'
            else if ($xstype = ('float', 'decimal',
                                'double'))              then 'Double'
            else if ($xstype = 'boolean')               then 'Boolean'
            else if ($xstype = 'date')                  then 'LocalDate'
            else if ($xstype = 'time')                  then 'LocalTime'
            else if ($xstype = 'dateTime')              then 'LocalDateTime'
            else if ($xstype = 'dateTimeStamp')         then 'OffsetDateTime'
            else                                             'String'
        "/>
    </xsl:function>

    <!--
        Function: cimtool:unique-columns
        Purpose:  Returns the sequence of column names that together form the
                  heuristic @UniqueConstraint for a non-IdentifiedObject entity
                  class. This mirrors identically the logic in the companion
                  sql-rdfs-ansi92 builder that emits:

                    ALTER TABLE "CurveData" ADD UNIQUE ("xvalue", "y1value", "Curve");

                  The constraint spans ALL required (minOccurs=1) non-surrogate
                  columns on the class, including:
                    - Required scalar attributes (a:Simple, a:Domain, minOccurs=1)
                    - Required single-valued FK associations (a:Instance, a:Reference,
                      a:Compound with minOccurs=1 and maxOccurs=1)

                  The surrogate "id" column is excluded — a UNIQUE on the PK is
                  redundant.

                  Why this heuristic is needed:
                    Non-IdentifiedObject classes carry a surrogate UUID "id" as their
                    primary key because the profile catalog XML does not encode which
                    subset of columns forms the true natural composite key — that
                    information exists only in the IEC specification prose. The
                    heuristic spans all required columns as a guard: the constraint
                    may be wider than the true natural key but is never incorrect.
                    A wider UNIQUE prevents valid duplicate insertion; a missing UNIQUE
                    permits silent semantic corruption.

                  This function is only meaningful for non-IdentifiedObject classes
                  (those for which cimtool:has-mrid-ancestor returns false). It is
                  never called for IdentifiedObject subclasses since those use their
                  natural mRID as the primary key and need no additional UNIQUE guard.

                  Note on subclasses: the function inspects only the direct children
                  of the given element. For subclasses such as PhaseTapChangerTablePoint
                  (extends TapChangerTablePoint), the required columns declared on the
                  subclass itself are captured. The parent class columns are handled
                  by the parent class's own @UniqueConstraint.

        Parameters:
            $element — The a:Root or a:ComplexType element to inspect
        Returns:
            xs:string* — Sequence of column name strings for the UNIQUE constraint.
                         Empty sequence if no required non-surrogate columns exist.
    -->
    <xsl:function name="cimtool:unique-columns" as="xs:string*">
        <xsl:param name="element" as="element()"/>
        <xsl:sequence select="
            distinct-values((
                (: Required scalar attributes — a:Simple and a:Domain with minOccurs=1 :)
                $element/(a:Simple|a:Domain)
                    [@minOccurs = '1']
                    [not(@maxOccurs) or @maxOccurs = '1']
                    [@name != 'mRID']
                    /string(@name),

                (: Required single-valued FK associations — a:Instance, a:Reference,
                   a:Compound with minOccurs=1 and maxOccurs=1.
                   The @name attribute is the FK column name in the database. :)
                $element/(a:Instance|a:Reference|a:Compound)
                    [@minOccurs = '1']
                    [not(@maxOccurs) or @maxOccurs = '1']
                    [@name != 'mRID']
                    /string(@name)
            ))
        "/>
    </xsl:function>

    <!--
        Function: cimtool:index-columns
        Purpose:  Returns the sequence of FK column names for which @Index entries
                  should be emitted on the @Table annotation. This mirrors exactly
                  the CREATE INDEX statements emitted by the companion
                  sql-rdfs-ansi92 builder:

                    CREATE INDEX ix_Equipment_EquipmentContainer
                        ON "Equipment" ("EquipmentContainer");

                  becomes in JPA:

                    @Table(name="Equipment", indexes = {
                        @Index(name="ix_Equipment_EquipmentContainer",
                               columnList="EquipmentContainer")
                    })

                  The index name convention is ix_{tableName}_{columnName},
                  identical to the SQL DDL builder, ensuring schema validation
                  (hbm2ddl.auto=validate) finds the expected index names.

                  All single-valued FK associations are indexed:
                    - a:Instance  — regular navigable associations
                    - a:Reference — by-reference associations
                    - a:Compound  — compound value object references (these
                      already carry a UNIQUE constraint, but an explicit @Index
                      maintains exact parity with the SQL DDL builder which
                      emits CREATE INDEX for all three types)

                  The mRID column is excluded — it is the primary key and
                  already has an implicit index from the PK constraint.
        Parameters:
            $element — The a:Root, a:ComplexType, or a:CompoundType element
        Returns:
            xs:string* — Sequence of FK column name strings
    -->
    <xsl:function name="cimtool:index-columns" as="xs:string*">
        <xsl:param name="element" as="element()"/>
        <xsl:sequence select="
            distinct-values(
                $element/(a:Instance|a:Reference|a:Compound)
                    [not(@maxOccurs) or @maxOccurs = '1']
                    [@name != 'mRID']
                    /string(@name)
            )
        "/>
    </xsl:function>

    <!--
    ════════════════════════════════════════════════════════════════════════════════
    BEGIN: TOPOLOGICAL SORT FUNCTIONS

    These six functions form a self-contained unit ported directly from the
    companion csharp-ef-rdfs.xsl builder. They operate purely on the CIMTool
    profile catalog XML structure (a:Catalog, a:CompoundType, a:Root,
    a:ComplexType, a:SuperType, a:Instance, a:Reference, a:Compound) and the
    XPath 3.1 map: namespace.

    They ensure that allClasses emits entity classes in dependency-safe order so
    that the JPA provider can bootstrap its entity model without encountering
    forward references. Specifically, a superclass must appear before any of its
    subclasses, and a class referenced by a FK association must appear before the
    class that holds the FK.

    Call sequence:
        cimtool:topological-sort
            └── cimtool:topological-sort-helper
                    └── cimtool:build-dependencies-map
                            └── cimtool:get-dependencies
                                    ├── cimtool:get-union-dependencies
                                    │       └── cimtool:inherits-from
                                    └── cimtool:inherits-from
        cimtool:create-exclusions-map   (used to seed the exclusion map passed
                                        into build-dependencies-map)
    ════════════════════════════════════════════════════════════════════════════════
    -->

    <!--
        Function: cimtool:inherits-from
        Purpose:  Checks if an element inherits from a given @baseClass by
                  recursively walking the a:SuperType chain.
        Parameters:
            $element         — The element to check (a:Root or a:ComplexType)
            $targetBaseClass — The @baseClass URI to search for in the hierarchy
        Returns:
            xs:boolean — true if $element is or inherits from $targetBaseClass
    -->
    <xsl:function name="cimtool:inherits-from" as="xs:boolean">
        <xsl:param name="element"         as="element()"/>
        <xsl:param name="targetBaseClass" as="xs:string"/>
        <xsl:sequence select="
            if ($element/@baseClass = $targetBaseClass) then
                true()
            else if ($element/a:SuperType) then
                let $superBaseClass := $element/a:SuperType/@baseClass,
                    $parent := (
                        root($element)//a:Root[@baseClass = $superBaseClass] |
                        root($element)//a:ComplexType[@baseClass = $superBaseClass]
                    )[1]
                return
                    if ($parent) then cimtool:inherits-from($parent, $targetBaseClass)
                    else false()
            else
                false()
        "/>
    </xsl:function>

    <!--
        Function: cimtool:get-union-dependencies
        Purpose:  Returns the @baseClass values of all concrete a:Root elements
                  that inherit from a given abstract a:ComplexType. Used to
                  resolve abstract class references to their concrete subtypes
                  when building the dependency graph.
        Parameters:
            $abstract-element — The a:ComplexType element (abstract class)
        Returns:
            xs:string* — Distinct sequence of @baseClass values for all
                         concrete subclasses
    -->
    <xsl:function name="cimtool:get-union-dependencies" as="xs:string*">
        <xsl:param name="abstract-element" as="element()"/>
        <xsl:sequence select="
            distinct-values(
                for $root in root($abstract-element)//a:Root
                return
                    if (cimtool:inherits-from($root, string($abstract-element/@baseClass)))
                    then string($root/@baseClass)
                    else ()
            )
        "/>
    </xsl:function>

    <!--
        Function: cimtool:get-dependencies
        Purpose:  Returns all distinct @baseClass dependency values for a single
                  element, including dependencies inherited through its a:SuperType
                  chain AND all FK associations (a:Instance, a:Reference). For
                  a:Instance/a:Reference pointing at abstract classes (a:ComplexType),
                  resolves to concrete subclass baseClasses via
                  cimtool:get-union-dependencies. Excludes types present in the
                  $exclusion-map (i.e. already-processed categories).

                  IMPORTANT — NOT USED FOR CLASS ORDERING IN THIS BUILDER:
                  This function is retained for completeness and structural parity
                  with the companion sql-rdfs-ansi92.xsl and csharp-ef-rdfs.xsl
                  builders, where it is appropriate. It is NOT called by
                  cimtool:build-dependencies-map in this JPA builder.

                  For JPA entity class ordering, only inheritance order is needed.
                  Java allows forward class references within a single compilation
                  unit, so FK associations do NOT impose any ordering constraint on
                  class declarations. Using full FK deps here creates cycles in
                  profiles with dense association graphs (e.g. CGMES CoreEquipment),
                  causing Kahn's algorithm to stall and fall back to document order
                  for 40+ classes. cimtool:get-inheritance-deps is used instead.

                  This function remains available for potential future use cases
                  that require full transitive dependency tracking.
        Parameters:
            $element       — The element to examine (a:Root, a:ComplexType, etc.)
            $exclusion-map — map(xs:string, xs:boolean) of baseClass values to exclude
        Returns:
            xs:string* — Distinct filtered sequence of dependency @baseClass values
    -->
    <xsl:function name="cimtool:get-dependencies" as="xs:string*">
        <xsl:param name="element"       as="element()"/>
        <xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>
        <xsl:sequence select="
            distinct-values((
                (: Non-association children with @baseClass — e.g. Compound references :)
                $element/*[@baseClass]
                    [not(self::a:SuperType)]
                    [not(self::a:Instance)]
                    [not(self::a:Reference)]
                    [not(self::a:InverseInstance)]
                    [not(self::a:InverseReference)]
                    /string(@baseClass)[not(map:contains($exclusion-map, .))],

                (: Instance/Reference children — resolve abstract classes to union members.
                   The [1] predicate guards against profiles where the same @baseClass URI
                   appears on more than one catalog element. :)
                for $assoc in $element/(a:Instance|a:Reference)[@baseClass]
                return
                    let $assoc-baseClass    := string($assoc/@baseClass),
                        $referenced-element := (root($element)/*/node()[@baseClass = $assoc-baseClass])[1]
                    return
                        if ($referenced-element/self::a:ComplexType) then
                            (
                                cimtool:get-union-dependencies($referenced-element)
                                    [not(map:contains($exclusion-map, .))],
                                if (not(map:contains($exclusion-map, $assoc-baseClass)))
                                then $assoc-baseClass
                                else ()
                            )
                        else if ($referenced-element/self::a:Root) then
                            if (not(map:contains($exclusion-map, $assoc-baseClass)))
                            then $assoc-baseClass
                            else ()
                        else
                            if (not(map:contains($exclusion-map, $assoc-baseClass)))
                            then $assoc-baseClass
                            else (),

                (: SuperType chain — the parent's own @baseClass must be emitted
                   as a direct dependency so it is placed before this element even
                   when the parent has no associations of its own. :)
                if ($element/a:SuperType) then
                    let $supertype-baseClass := string($element/a:SuperType/@baseClass),
                        $parent := (root($element)/*/node()[@baseClass = $supertype-baseClass])[1]
                    return (
                        if (not(map:contains($exclusion-map, $supertype-baseClass)))
                        then $supertype-baseClass
                        else (),
                        if ($parent)
                        then cimtool:get-dependencies($parent, $exclusion-map)
                        else ()
                    )
                else
                    ()
            ))
        "/>
    </xsl:function>

    <!--
        Function: cimtool:get-inheritance-deps
        Purpose:  Returns the @baseClass dependency values needed for topological
                  sorting of JPA entity classes. Unlike the full cimtool:get-dependencies
                  function (which also tracks FK associations), this function tracks ONLY
                  the direct SuperType parent dependency.

                  This is intentionally simpler than cimtool:get-dependencies. For JPA
                  class declaration ordering, the only strict constraint is that each
                  superclass appears before its subclasses. FK associations do NOT need
                  to be respected because Java allows forward class references within a
                  single compilation unit — the JPA provider resolves associations by
                  class name, not declaration order.

                  Using the full cimtool:get-dependencies for JPA ordering creates false
                  cycles in profiles with dense association graphs (e.g. CGMES CoreEquipment
                  where OperationalLimitSet → Equipment → Terminal → ACDCConverter →
                  ... loops back through the association network). These cycles cause
                  Kahn's algorithm to stall and fall back to document order for 40+
                  classes, defeating the purpose of the sort.

                  By tracking only inheritance, this function guarantees a cycle-free
                  dependency graph and produces a correct inheritance-ordered output
                  for all CIM profiles.
        Parameters:
            $element       — The element to examine (a:Root or a:ComplexType)
            $exclusion-map — map(xs:string, xs:boolean) of baseClass values to exclude
                             (already-processed tiers such as EnumeratedType)
        Returns:
            xs:string* — Zero or one @baseClass string (the direct parent, if any
                         and if not excluded)
    -->
    <xsl:function name="cimtool:get-inheritance-deps" as="xs:string*">
        <xsl:param name="element"       as="element()"/>
        <xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>
        <xsl:sequence select="
            if ($element/a:SuperType) then
                let $supertype-baseClass := string($element/a:SuperType[1]/@baseClass)
                return
                    if (not(map:contains($exclusion-map, $supertype-baseClass)))
                    then $supertype-baseClass
                    else ()
            else
                ()
        "/>
    </xsl:function>

    <!--
        Function: cimtool:create-exclusions-map
        Purpose:  Builds a map(xs:string, xs:boolean) keyed on @baseClass values
                  for a set of already-processed elements. Passed into
                  cimtool:build-dependencies-map to exclude those types from the
                  dependency graph of the next processing tier.
        Parameters:
            $elements — Elements whose @baseClass values should be excluded
        Returns:
            map(xs:string, xs:boolean) — Key: @baseClass value, Value: true()
    -->
    <xsl:function name="cimtool:create-exclusions-map" as="map(xs:string, xs:boolean)">
        <xsl:param name="elements" as="element()*"/>
        <xsl:sequence select="
            map:merge(
                for $element in $elements[@baseClass]
                return map:entry(string($element/@baseClass), true())
            )
        "/>
    </xsl:function>

    <!--
        Function: cimtool:build-dependencies-map
        Purpose:  Builds a map(xs:string, xs:string*) representing the full
                  dependency graph for a set of elements using
                  cimtool:get-dependencies (which tracks both inheritance AND
                  FK/Compound associations). Each entry maps an element's
                  @baseClass to the sequence of @baseClass values it depends on.

                  Used for Tier 2 (CompoundType) sorting. Compound association
                  networks are shallow and do not create cycles, so full dep
                  tracking produces correct ordering.

                  For Tier 3 (ComplexType + Root) sorting, use
                  cimtool:build-inheritance-map instead — see that function's
                  documentation for why inheritance-only deps are needed there.
        Parameters:
            $elements      — Elements to include in the dependency graph
            $exclusion-map — map(xs:string, xs:boolean) of already-processed types
        Returns:
            map(xs:string, xs:string*) — Key: @baseClass, Value: dependency sequence
    -->
    <xsl:function name="cimtool:build-dependencies-map" as="map(xs:string, xs:string*)">
        <xsl:param name="elements"      as="element()*"/>
        <xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>
        <xsl:sequence select="
            map:merge(
                for $type in $elements[@baseClass]
                return map:entry(
                    string($type/@baseClass),
                    cimtool:get-dependencies($type, $exclusion-map)
                )
            )
        "/>
    </xsl:function>

    <!--
        Function: cimtool:build-inheritance-map
        Purpose:  Variant of cimtool:build-dependencies-map that uses
                  cimtool:get-inheritance-deps instead of cimtool:get-dependencies.
                  Used for Tier 3 (ComplexType + Root) sorting where only inheritance
                  order is required — see cimtool:get-inheritance-deps for rationale.
        Parameters:
            $elements      — Elements to include in the dependency graph
            $exclusion-map — map(xs:string, xs:boolean) of already-processed types
        Returns:
            map(xs:string, xs:string*) — Key: @baseClass, Value: dependency sequence
    -->
    <xsl:function name="cimtool:build-inheritance-map" as="map(xs:string, xs:string*)">
        <xsl:param name="elements"      as="element()*"/>
        <xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>
        <xsl:sequence select="
            map:merge(
                for $type in $elements[@baseClass]
                return map:entry(
                    string($type/@baseClass),
                    cimtool:get-inheritance-deps($type, $exclusion-map)
                )
            )
        "/>
    </xsl:function>

    <!--
        Function: cimtool:topological-sort
        Purpose:  Entry point for topological sort. Sorts a sequence of elements
                  into dependency order so that each element appears only after
                  all elements it depends on. Falls back to document order for
                  any elements involved in circular dependencies.
        Parameters:
            $elements  — Elements to sort (e.g. //a:ComplexType|//a:Root)
            $deps-map  — Dependency map from cimtool:build-dependencies-map
        Returns:
            element()* — Elements in topological (dependency-safe) order
    -->
    <xsl:function name="cimtool:topological-sort" as="element()*">
        <xsl:param name="elements"  as="element()*"/>
        <xsl:param name="deps-map"  as="map(xs:string, xs:string*)"/>
        <xsl:sequence select="cimtool:topological-sort-helper($elements, $deps-map, ())"/>
    </xsl:function>

    <!--
        Function: cimtool:topological-sort-helper
        Purpose:  Recursive Kahn's-algorithm implementation. On each pass, finds
                  all elements whose dependencies are fully satisfied by the
                  already-sorted set, adds them to the sorted sequence, and
                  recurses on the remainder. Terminates when all elements are
                  sorted or a circular dependency is detected (fallback to
                  document order for the remaining unsortable elements).
        Parameters:
            $remaining — Elements not yet placed in the sorted output
            $deps-map  — Dependency map
            $sorted    — Accumulated sorted elements (initially empty)
        Returns:
            element()* — Fully sorted sequence
    -->
    <xsl:function name="cimtool:topological-sort-helper" as="element()*">
        <xsl:param name="remaining" as="element()*"/>
        <xsl:param name="deps-map"  as="map(xs:string, xs:string*)"/>
        <xsl:param name="sorted"    as="element()*"/>
        <xsl:choose>
            <xsl:when test="fn:empty($remaining)">
                <xsl:sequence select="$sorted"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="sorted-baseClasses" select="
                    for $elem in $sorted return string($elem/@baseClass)
                "/>
                <xsl:variable name="ready-elements" select="
                    for $elem in $remaining
                    return
                        let $elem-baseClass := fn:string($elem/@baseClass),
                            $elem-deps      := $deps-map($elem-baseClass)
                        return
                            if (every $dep in $elem-deps satisfies $dep = $sorted-baseClasses)
                            then $elem
                            else ()
                "/>
                <xsl:choose>
                    <xsl:when test="fn:exists($ready-elements)">
                        <xsl:sequence select="cimtool:topological-sort-helper(
                            $remaining except $ready-elements,
                            $deps-map,
                            ($sorted, $ready-elements)
                        )"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- Circular dependency detected — append remaining in document order -->
                        <xsl:sequence select="$sorted"/>
                        <xsl:sequence select="$remaining"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
    ════════════════════════════════════════════════════════════════════════════════
    END: TOPOLOGICAL SORT FUNCTIONS
    ════════════════════════════════════════════════════════════════════════════════
    -->
    
    <xsl:variable name="lc">abcdefghijklmnopqrstuvwxyz</xsl:variable>
    <xsl:variable name="uc">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>

    <!--
        Function: cimtool:capitalize
        Uppercases the first character of $name and prepends an optional $prefix.
        Examples:
            cimtool:capitalize('voltageLevel', '')    -> 'VoltageLevel'
            cimtool:capitalize('voltageLevel', 'get') -> 'getVoltageLevel'
    -->
    <xsl:function name="cimtool:capitalize" as="xs:string">
        <xsl:param name="name"   as="xs:string"/>
        <xsl:param name="prefix" as="xs:string"/>
        <xsl:sequence select="concat($prefix, translate(substring($name,1,1),$lc,$uc), substring($name,2))"/>
    </xsl:function>

    <!--
        Function: cimtool:uncapitalize
        Lowercases the first character of $name.
        Example:
            cimtool:uncapitalize('VoltageLevel') -> 'voltageLevel'
    -->
    <xsl:function name="cimtool:uncapitalize" as="xs:string">
        <xsl:param name="name" as="xs:string"/>
        <xsl:sequence select="concat(translate(substring($name,1,1),$uc,$lc), substring($name,2))"/>
    </xsl:function>

    <!--
        Emits a Java getter, setter, annotation block and private field for a single property.
        Parameters:
            name     — property name (becomes field name after uncapitalize)
            type     — Java type string (e.g. String, Double, Boolean)
            xstype   — XSD type used to derive Java type if $type not supplied
            annotate — pre-built annotation XML nodes to emit before the field
            nullable — if 'false' emits nullable=false on @Column
    -->
    <xsl:template name="property">
        <xsl:param name="name"/>
        <xsl:param name="xstype"/>
        <xsl:param name="type">
            <xsl:value-of select="cimtool:javaType(string($xstype))"/>
        </xsl:param>
        <xsl:param name="annotate"/>
        <xsl:param name="prop">
            <xsl:value-of select="translate($name,'_','')"/>
        </xsl:param>
        <xsl:variable name="field"   select="cimtool:uncapitalize($prop)"/>
        <xsl:variable name="getter"  select="cimtool:capitalize($prop, 'get')"/>
        <xsl:variable name="setter"  select="cimtool:capitalize($prop, 'set')"/>
        <xsl:copy-of select="$annotate"/>
        <item>private <xsl:value-of select="concat($type, ' ', $field)"/>;</item>
        <item></item>       
        <list begin="public {$type} {$getter}() {{" indent="    " end="}}">
            <item>return <xsl:value-of select="$field"/>;</item>
        </list>
        <item></item>
        <list begin="public void {$setter}({$type} {$field}) {{" indent="    " end="}}">
            <item>this.<xsl:value-of select="$field"/> = <xsl:value-of select="$field"/>;</item>
        </list>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- Top-level Catalog template                                          -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template match="a:Catalog">

        <!-- Topological sort — computed once, reused for class output      -->
        <!-- and allClasses array to guarantee dependency-safe ordering.    -->

        <!-- Tier 1: EnumeratedType — no dependencies, emitted in document order -->

        <!-- Tier 2: CompoundType — exclude EnumeratedTypes.
             Uses full cimtool:build-dependencies-map because compound association
             networks are shallow and do not create cycles. -->
        <xsl:variable name="compound-exclusions"
            select="cimtool:create-exclusions-map(//a:EnumeratedType)"/>
        <xsl:variable name="compound-deps-map"
            select="cimtool:build-dependencies-map(//a:CompoundType, $compound-exclusions)"/>
        <xsl:variable name="sorted-compounds"
            select="cimtool:topological-sort(//a:CompoundType, $compound-deps-map)"/>

        <!-- Tier 3: ComplexType + Root — exclude EnumeratedTypes and CompoundTypes.
             Uses cimtool:build-inheritance-map (inheritance-only deps) to avoid
             cycles in dense association graphs. -->
        <xsl:variable name="class-exclusions"
            select="cimtool:create-exclusions-map(//a:EnumeratedType|//a:CompoundType)"/>
        <xsl:variable name="class-deps-map"
            select="cimtool:build-inheritance-map(//a:ComplexType|//a:Root, $class-exclusions)"/>
        <xsl:variable name="sorted-classes"
            select="cimtool:topological-sort(//a:ComplexType|//a:Root, $class-deps-map)"/>

        <document>
            <item>package <xsl:value-of select="$package"/>;</item>
            <item>import jakarta.persistence.*;</item>
            <item>import java.time.*;</item>
            <item>import java.util.*;</item>
            <list begin="/**" indent=" * " end=" */">
                <item>Annotated Java for the <xsl:value-of select="$envelope"/> profile.</item>
                <item>Generated by CIMTool https://cimtool.ucaiug.io</item>
                <item></item>
                <item>JPA Version: Jakarta Persistence 3.1 (Jakarta EE 10)</item>
                <item></item>
                <list begin="Inheritance Strategy" indent="  " end="">
                    <item>All entity hierarchies use JOINED table-per-type inheritance, matching</item>
                    <item>the SQL DDL schema generated by the companion sql-rdfs-ansi92 builder.</item>
                    <item>Each class maps to its own table joined on the primary key.</item>
                </list>
                <item></item>
                <list begin="Class Ordering — Topological Sort" indent="  " end="">
                    <item>Entity classes and the allClasses array are emitted in inheritance-safe</item>
                    <item>topological order using Kahn's algorithm, ensuring every superclass</item>
                    <item>appears before its subclasses. EnumeratedTypes are always emitted first</item>
                    <item>as they have no dependencies. ComplexType and Root classes follow in</item>
                    <item>inheritance-sorted order.</item>
                    <item></item>
                    <item>Note: only the SuperType (inheritance) chain is used for ordering — FK</item>
                    <item>associations are intentionally excluded. Java allows forward class</item>
                    <item>references within a single compilation unit, so FK association order</item>
                    <item>is irrelevant for class declarations. Including FK associations in the</item>
                    <item>dependency graph creates cycles in profiles with dense association</item>
                    <item>networks (e.g. CGMES) that cause 40+ classes to fall back to document</item>
                    <item>order — precisely the problem this sort is designed to prevent.</item>
                </list>
                <item></item>
                <list begin="IdentifiedObject Hierarchy vs Non-IdentifiedObject Classes" indent="  " end="">
                    <item>Classes that inherit from IdentifiedObject use a natural String mRID</item>
                    <item>primary key mapped to @Id @Column(name="mRID").</item>
                    <item>Classes that do NOT inherit from IdentifiedObject (e.g. CurveData,</item>
                    <item>TapChangerTablePoint, Quality61850) use a surrogate UUID primary key</item>
                    <item>mapped to @Id @GeneratedValue(strategy=GenerationType.UUID) with</item>
                    <item>@Column(name="id"). The UUID is auto-generated by the JPA provider</item>
                    <item>on persist, consistent with the companion SQL DDL schema.</item>
                </list>
                <item></item>
                <list begin="FK Column Indexes" indent="  " end="">
                    <item>All single-valued FK associations (a:Instance, a:Reference, a:Compound)</item>
                    <item>receive a corresponding @Index entry on the @Table annotation. This</item>
                    <item>mirrors exactly the CREATE INDEX statements emitted by the companion</item>
                    <item>sql-rdfs-ansi92 builder, using the same naming convention:</item>
                    <item></item>
					<list begin="" indent="  " end="">
						<item>ix_{tableName}_{columnName}</item>
					</list>
                    <item></item>
					<list begin="For example:" indent="  " end="">
						<item>CREATE INDEX ix_Equipment_EquipmentContainer ...</item>
					</list>
					<list begin="becomes:" indent="  " end="">
						 <item>@Index(name="ix_Equipment_EquipmentContainer", columnList="EquipmentContainer")</item>
					</list>
                    <item></item>
                    <item>Consistent naming ensures schema validation (hbm2ddl.auto=validate)</item>
                    <item>finds the expected index names in the database. Compound FK columns</item>
                    <item>are also indexed even though they already carry a UNIQUE constraint —</item>
                    <item>this maintains exact parity with the SQL DDL builder.</item>
                </list>
                <item></item>
                <list begin="Heuristic Unique Constraints for Non-IdentifiedObject Classes" indent="  " end="">
                    <item>Non-IdentifiedObject entity classes (those using a surrogate UUID @Id)</item>
                    <item>receive a @UniqueConstraint on their @Table annotation spanning all</item>
                    <item>required (minOccurs=1) non-surrogate columns — both scalar attributes</item>
                    <item>and required single-valued FK associations. This mirrors the heuristic</item>
                    <item>UNIQUE constraint emitted by the companion sql-rdfs-ansi92 builder:</item>
                    <item></item>
					<list begin="" indent="  " end="">
						<item>ALTER TABLE "CurveData" ADD UNIQUE ("xvalue", "y1value", "Curve");</item>
					</list>
                    <item></item>
					<list begin="becomes in JPA:" indent="  " end="">
						<list begin="@Table(name=&quot;CurveData&quot;, uniqueConstraints = {{" indent="  " end="}})">
							<item>@UniqueConstraint(columnNames = {"xvalue", "y1value", "Curve"})</item>
						</list>
					</list>
                    <item></item>
                    <item>The constraint may be wider than the true natural key (which is only</item>
                    <item>known from the IEC specification prose, not from the profile catalog</item>
                    <item>XML) but is never incorrect — a wider UNIQUE prevents valid duplicate</item>
                    <item>insertion; a missing UNIQUE permits silent semantic corruption.</item>
                    <item>IdentifiedObject subclasses are not affected — their natural mRID PK</item>
                    <item>is the authoritative unique identifier and needs no additional guard.</item>
                </list>
                <item></item>
                <list begin="Boolean Column Mapping" indent="  " end="">
                    <item>CIM boolean attributes (xstype="boolean") map to Java Boolean and are</item>
                    <item>annotated with @Column(columnDefinition="INTEGER DEFAULT 0"). Without</item>
                    <item>this hint most JPA providers create a BOOLEAN or BIT column, which does</item>
                    <item>not match the companion sql-rdfs-ansi92 builder's schema:</item>
                    <item></item>
					<list begin="" indent="  " end="">
						<item>"enabled" INTEGER DEFAULT 0 CHECK ("enabled" IN (0, 1))</item>
					</list>
                    <item></item>
                    <item>The columnDefinition aligns the JPA-managed schema with the SQL DDL.</item>
                    <item>Required booleans (minOccurs=1) additionally carry nullable=false,</item>
                    <item>matching the NOT NULL emitted by the SQL DDL builder for those columns.</item>
                    <item></item>
                    <item>Oracle portability note: columnDefinition="INTEGER DEFAULT 0" is passed</item>
                    <item>verbatim to the RDBMS during JPA schema generation. On Oracle, INTEGER</item>
                    <item>maps to NUMBER(38) rather than the idiomatic NUMBER(1). If targeting</item>
                    <item>Oracle and using JPA schema generation, override columnDefinition to</item>
                    <item>NUMBER(1) DEFAULT 0 in a subclass or use a custom AttributeConverter.</item>
                    <item>This concern does not apply when using the companion sql-rdfs-ansi92</item>
                    <item>DDL script for schema creation (the recommended approach) — in that</item>
                    <item>case the columnDefinition hint is advisory only and never executed.</item>
                </list>
                <item></item>
                <list begin="Requirements related to equals() and hashCode()" indent="  " end="">
                    <item>All entity classes include generated equals() and hashCode()</item>
                    <item>implementations following the Mihalcea portable JPA pattern:</item>
                    <item></item>
                    <list begin="" indent="  " end="">
                        <item>- equals() uses getClass() rather than instanceof to correctly</item>
                        <list begin="" indent="  " end="">
							<item>handle Hibernate proxy objects. instanceof would pass for the</item>
							<item>proxy-to-real direction but fail in reverse, causing subtle bugs</item>
							<item>when entities are stored in collections.</item>
                        </list>
                        <item></item>
                        <item>- hashCode() returns getClass().hashCode() — a fixed constant per</item>
                        <list begin="" indent="  " end="">
							<item>class. This ensures hash stability across the transient-to-</item>
							<item>persistent lifecycle before the JPA provider assigns the key.</item>
						</list>
                        <item></item>
                        <item>- equals() compares on the natural key only when non-null, so two</item>
                        <list begin="" indent="  " end="">
							<item>  unsaved (transient) instances are never considered equal.</item>
						</list>
                    </list>
                    <item></item>
                    <list begin="Generated on hierarchy roots only — subclasses inherit:" indent="  " end="">
                        <item>- IdentifiedObject root: key = mRID (String)</item>
                        <item>- Non-IdentifiedObject roots: key = id (UUID)</item>
                        <item>- CompoundType classes: key = id (UUID)</item>
                        <item>- EnumeratedType classes: key = name (String)</item>
                    </list>
                    <item></item>
                    <item>Reference: Vlad Mihalcea, "The Best Way to Implement equals and</item>
                    <item>hashCode with JPA":</item>
                    <list begin="" indent="  " end="">
						<item>https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/</item>
					</list>
                </list>
                <item></item>
                <list begin="@Inheritance on Non-IdentifiedObject Classes" indent="  " end="">
                    <item>The @Inheritance(strategy=InheritanceType.JOINED) annotation is</item>
                    <item>only emitted on non-IdentifiedObject root classes that have at</item>
                    <item>least one subclass present in this profile. Leaf classes with no</item>
                    <item>subclasses (e.g. CurveData, RegularTimePoint) are emitted without</item>
                    <item>@Inheritance — it would be misleading to the JPA provider and</item>
                    <item>adds no functional value for a class that is never extended.</item>
                    <item>IdentifiedObject is always emitted with @Inheritance since it is</item>
                    <item>by definition the root of the entire entity hierarchy.</item>
                </list>
                <item></item>
                <list begin="No @OneToMany Mappings — Intentional Design" indent="  " end="">
                    <item>Collection associations (maxOccurs=unbounded, i.e. the one side of a</item>
                    <item>one-to-many relationship) are intentionally NOT mapped as @OneToMany.</item>
                    <item>The @ManyToOne on the child side is sufficient for all JPA operations.</item>
                    <item>This is a deliberate architectural choice, not an omission. Developers</item>
                    <item>who expect @OneToMany collection properties should read this section</item>
                    <item>before adding them manually.</item>
                    <item></item>
                    <item>Rationale:</item>
                    <list begin="" indent="  " end="">
                        <item>- @OneToMany requires a mappedBy string that is not validated at</item>
                        <list begin="" indent="  " end="">
                            <item>compile time. A mismatch fails silently and throws an obscure</item>
                            <item>AnnotationException at JPA provider bootstrap, which is difficult</item>
                            <item>to diagnose in generated code.</item>
                        </list>
                        <item>- CIM associations are unbounded by definition. Loading an unbounded</item>
                        <list begin="" indent="  " end="">
                            <item>collection into memory with no pagination is a runtime hazard for</item>
                            <item>large profiles such as CGMES CoreEquipment, where an equipment</item>
                            <item>container may have thousands of children. Hibernate will issue a</item>
                            <item>single SELECT with no LIMIT, materialising the entire result set.</item>
                        </list>
                        <item>- Bidirectional @OneToMany requires both sides to be kept in sync</item>
                        <list begin="" indent="  " end="">
                            <item>on every add and remove operation. Failing to do so causes</item>
                            <item>inconsistent in-memory state and subtle persistence bugs that</item>
                            <item>are notoriously difficult to reproduce and debug.</item>
                        </list>
                        <item>- Bidirectional mappings with CascadeType.ALL or orphanRemoval=true</item>
                        <list begin="" indent="  " end="">
                            <item>interact unpredictably with the compound value object cascade</item>
                            <item>semantics already present in this generated code, risking</item>
                            <item>accidental deletion of entities that should remain.</item>
                        </list>
                        <item>- Inverse associations are profile-dependent and not present in all</item>
                        <list begin="" indent="  " end="">
                            <item>profiles, making generated @OneToMany properties inconsistent</item>
                            <item>across profiles and creating a false expectation of completeness.</item>
                        </list>
                        <item>- Explicit JPQL queries support pagination, filtering, and sorting</item>
                        <list begin="" indent="  " end="">
                            <item>that a generated collection property never can, and make the</item>
                            <item>data access intent explicit rather than implicit.</item>
                        </list>
                    </list>
                    <item></item>
                    <item>To navigate from a parent entity to its children, use a JPQL query.</item>
                    <item>Query through the child side via the @ManyToOne FK field. This keeps</item>
                    <item>all data access explicit, pageable, and safe for large result sets:</item>
                    <item></item>
                    <list begin="" indent="  " end="">
                        <item>// Find all PowerElectronicsUnit instances for a given connection:</item>
                        <list begin="List&lt;PowerElectronicsUnit&gt; units = em.createQuery(" indent="  " end="">
                            <item>"SELECT u FROM PowerElectronicsUnit u " +</item>
                            <item>"WHERE u.powerElectronicsConnection = :conn", PowerElectronicsUnit.class)</item>
                            <list begin="" indent="  " end="">
                                <item>.setParameter("conn", connection)</item>
                                <item>.getResultList();</item>
                            </list>
                        </list>
                        <item></item>
                        <item>// With pagination for large result sets:</item>
                        <list begin="List&lt;Equipment&gt; equipment = em.createQuery(" indent="  " end="">
                            <item>"SELECT e FROM Equipment e " +</item>
                            <item>"WHERE e.equipmentContainer = :container", Equipment.class)</item>
                            <list begin="" indent="  " end="">
                                <item>.setParameter("container", substation)</item>
                                <item>.setFirstResult(0)</item>
                                <item>.setMaxResults(100)</item>
                                <item>.getResultList();</item>
                            </list>
                        </list>
                    </list>
                    <item></item>
                    <item>References:</item>
                    <list begin="" indent="  " end="">
                        <item>Vlad Mihalcea, "The Best Way to Map a @OneToMany Relationship</item>
                        <item>with JPA and Hibernate":</item>
                        <item>https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/</item>
                        <item></item>
                        <item>Vlad Mihalcea, "The Best Way to Use the @ManyToOne Annotation":</item>
                        <item>https://vladmihalcea.com/manytoone-jpa-hibernate/</item>
                    </list>
                </list>
                <item></item>
                <list begin="Compound Type References" indent="  " end="">
                    <item>CIM Compound types are value objects — each compound row has exactly</item>
                    <item>one owner (the specific parent column on the specific parent row that</item>
                    <item>caused its creation) and is never shared. They are represented as</item>
                    <item>separate @Entity classes with a surrogate UUID @Id auto-generated by</item>
                    <item>the JPA provider on persist via @GeneratedValue(strategy=UUID).</item>
                    <item></item>
                    <item>Compound references on parent entities use:</item>
                    <list begin="" indent="  " end="">
                        <item>@ManyToOne(fetch=FetchType.LAZY, cascade=CascadeType.REMOVE)</item>
                        <item>@JoinColumn(name="compoundColumnName")</item>
                    </list>
                    <item></item>
                    <item>CascadeType.REMOVE implements the value object ownership model —</item>
                    <item>when a parent entity is deleted, its compound child entities are</item>
                    <item>automatically deleted by the JPA provider, consistent with the</item>
                    <item>ON DELETE CASCADE constraints in the companion SQL DDL script.</item>
                    <item></item>
                    <item>Compound surrogate key UUID assignment is handled automatically by</item>
                    <item>the JPA provider via @GeneratedValue — no application-level UUID</item>
                    <item>generation code is required. See the companion sql-rdfs-ansi92 DDL</item>
                    <item>script header for full details on the value object ownership model</item>
                    <item>and the UUID generation directive.</item>
                </list>
                <item></item>
                <list begin="JPA Integration" indent="  " end="">
                    <item>This file includes complete JPA 3.1 annotations on all generated entity</item>
                    <item>classes. Unlike C#'s Entity Framework (EF) Core which requires explicit</item>
                    <item>ModelConfiguration registration, JPA discovers all mappings — relationships,</item>
                    <item>column definitions, inheritance strategy, and cascade behaviour — directly</item>
                    <item>from the annotations on the generated classes. No additional configuration</item>
                    <item>method needs to be called.</item>
                    <item></item>
                    <item>To integrate these entities into a JPA application, define a persistence</item>
                    <item>unit in META-INF/persistence.xml. The allClasses array at the bottom of</item>
                    <item>this file provides the complete list of entity classes for this profile.</item>
                    <item>For profiles with many classes, package scanning is strongly preferred</item>
                    <item>over explicit &lt;class&gt; entries — maintaining 100+ class entries manually</item>
                    <item>is impractical and any regeneration would require reconciling the list.</item>
                    <item></item>
                    <item>Note: all generated entity classes are static inner classes of the outer</item>
                    <item><xsl:value-of select="$envelope"/> class. When using explicit &lt;class&gt; entries in persistence.xml</item>
                    <item>use the $ separator, e.g.:</item>
                    <item></item>
                    <list begin="" indent="  " end="">
						<item><xsl:value-of select="$package"/>.<xsl:value-of select="$envelope"/>$IdentifiedObject</item>
					</list>
                    <item></item>
					<list begin="Hibernate 6.x (JPA 3.1 reference implementation):" indent="  " end="">
						<list begin="&lt;persistence-unit name=&quot;{$envelope}&quot; transaction-type=&quot;RESOURCE_LOCAL&quot;&gt;" indent="  " end="&lt;/persistence-unit&gt;">
							<item>&lt;provider&gt;org.hibernate.jpa.HibernatePersistenceProvider&lt;/provider&gt;</item>
							<item>&lt;jar-file&gt;cim-entities.jar&lt;/jar-file&gt;</item>
							<item>&lt;exclude-unlisted-classes&gt;false&lt;/exclude-unlisted-classes&gt;</item>
							<item>&lt;properties&gt;</item>
							<list begin="" indent="  " end="">
								<item>&lt;property name="jakarta.persistence.jdbc.url" value="jdbc:..."/&gt;</item>
								<item>&lt;property name="hibernate.hbm2ddl.auto" value="validate"/&gt;</item>
							</list>
							<item>&lt;/properties&gt;</item>
						</list>
					</list>
					<item></item>
					<list begin="EclipseLink (default provider in GlassFish, Payara):" indent="  " end="">
						<list begin="&lt;persistence-unit name=&quot;{$envelope}&quot; transaction-type=&quot;RESOURCE_LOCAL&quot;&gt;" indent="  " end="&lt;/persistence-unit&gt;">
							<item>&lt;provider&gt;org.eclipse.persistence.jpa.PersistenceProvider&lt;/provider&gt;</item>
							<item>&lt;jar-file&gt;cim-entities.jar&lt;/jar-file&gt;</item>
							<item>&lt;properties&gt;</item>
							<list begin="" indent="  " end="">
								<item>&lt;property name="jakarta.persistence.jdbc.url" value="jdbc:..."/&gt;</item>
								<item>&lt;property name="eclipselink.ddl-generation" value="none"/&gt;</item>
							</list>
							<item>&lt;/properties&gt;</item>
						</list>
					</list>
					<item></item>
					<list begin="OpenJPA (Apache TomEE):" indent="  " end="">
						<list begin="&lt;persistence-unit name=&quot;{$envelope}&quot; transaction-type=&quot;RESOURCE_LOCAL&quot;&gt;" indent="  " end="&lt;/persistence-unit&gt;">
							<item>&lt;provider&gt;org.apache.openjpa.persistence.PersistenceProviderImpl&lt;/provider&gt;</item>
							<item>&lt;jar-file&gt;cim-entities.jar&lt;/jar-file&gt;</item>
						</list>
					</list>
					<item></item>
					<list begin="Standalone Java application (any provider):" indent="  " end="">
						<item>// Use the EntityManagerFactory directly via Persistence.createEntityManagerFactory.</item>
						<item>// This is the most portable approach and works with any JPA provider on the classpath.</item>
						<item></item>
						<item>import jakarta.persistence.*;</item>
						<item></item>
						<item>EntityManagerFactory emFactory = Persistence.createEntityManagerFactory("<xsl:value-of select="$envelope"/>");</item>
						<list begin="try {{" indent="  " end="}}">
							<item>EntityManager em = emf.createEntityManager();</item>
							<item>em.getTransaction().begin();</item>
							<item>// ... work with entities ...</item>
							<item>em.getTransaction().commit();</item>
						</list>
						<list begin="finally {{" indent="  " end="}}">
							<item>em.close();</item>
						</list>
						<item>emFactory.close()</item>
					</list>
					<item></item>
					<list begin="Spring Boot:" indent="  " end="">
						<item>@SpringBootApplication</item>
						<item>@EntityScan("<xsl:value-of select="$package"/>")</item>
						<list begin="public class CimRepository {{" indent="  " end="}}">
							<item>@PersistenceContext(unitName = "<xsl:value-of select="$envelope"/>")</item>
							<item>private EntityManager em;</item>
						</list>
						<item></item>
						<item>// application.yml:</item>
						<item>spring.jpa.hibernate.ddl-auto: validate</item>
					</list>
					<item></item>
					<list begin="Quarkus:" indent="  " end="">
						<item>// application.properties:</item>
						<item>quarkus.hibernate-orm.packages=<xsl:value-of select="$package"/></item>
						<item>quarkus.hibernate-orm.database.generation=none</item>
					</list>
					<item></item>
					<list begin="Jakarta EE application server (WildFly, Payara, WebLogic):" indent="  " end="">
						<item>Use container-managed transactions and @PersistenceContext injection.</item>
						<item>Set transaction-type="JTA" with &lt;jta-data-source&gt; in persistence.xml.</item>
						<item>On servers with strict classloading (WebLogic, older WebSphere), use</item>
						<item>explicit &lt;class&gt; entries or &lt;jar-file&gt; rather than package scanning.</item>
						<item></item>
						<item>@Stateless</item>
						<list begin="public class CimRepository {{" indent="  " end="}}">
							<item>@PersistenceContext(unitName = "<xsl:value-of select="$envelope"/>")</item>
							<item>private EntityManager em;</item>
						</list>
					</list>
					<item></item>
                </list>
                <list begin="Schema Generation Warning" indent="  " end="">
                    <item>The companion sql-rdfs-ansi92 DDL script is the authoritative schema and</item>
                    <item>should always be used for production schema creation. JPA auto DDL</item>
                    <item>generation (hbm2ddl.auto, ddl-generation, etc.) will not reproduce:</item>
                    <item></item>
                    <list begin="" indent="  " end="">
						<item>- INTEGER DEFAULT 0 CHECK ("col" IN (0,1)) for boolean columns</item>
						<item>- Heuristic UNIQUE constraints on non-IdentifiedObject tables</item>
						<item>- Reverse-reference ON DELETE CASCADE for compound value objects</item>
                    </list>
                    <item></item>
                    <item>Set ddl-generation/hbm2ddl.auto to "validate" or "none" in all</item>
                    <item>environments where the SQL DDL script has been applied.</item>
                </list>
            </list>
            <list begin="public class {$envelope} {{" indent="    " end="}}">
                <!-- EnumeratedTypes first — no dependencies -->
                <xsl:apply-templates select="//a:EnumeratedType"/>
                <!-- CompoundTypes in topological order (after enums, before classes) -->
                <xsl:apply-templates select="$sorted-compounds"/>
                <!-- ComplexType + Root in topological order -->
                <xsl:apply-templates select="$sorted-classes"/>
                <xsl:call-template name="config">
                    <xsl:with-param name="sorted-compounds" select="$sorted-compounds"/>
                    <xsl:with-param name="sorted-classes"   select="$sorted-classes"/>
                </xsl:call-template>
            </list>
        </document>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- allClasses array — emitted in the same topological order as the    -->
    <!-- class output above, guaranteeing the JPA provider sees a           -->
    <!-- dependency-safe sequence when bootstrapping the entity model.      -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template name="config">
        <xsl:param name="sorted-compounds" as="element()*"/>
        <xsl:param name="sorted-classes"   as="element()*"/>
        <item></item>
        <item>public static final Class[] allClasses = new Class[]</item>
        <list begin="{{" indent="    " delim="," end="}};">
            <!-- EnumeratedTypes first — no dependencies -->
            <xsl:apply-templates select="//a:EnumeratedType" mode="config"/>
            <!-- CompoundTypes in the same topological order as class output -->
            <xsl:apply-templates select="$sorted-compounds" mode="config"/>
            <!-- ComplexType + Root in the same topological order as class output -->
            <xsl:apply-templates select="$sorted-classes" mode="config"/>
        </list>
    </xsl:template>

    <xsl:template match="a:ComplexType|a:Root|a:CompoundType|a:EnumeratedType" mode="config">
        <item><xsl:value-of select="@name"/>.class</item>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- ComplexType and Root class template                                  -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template match="a:ComplexType|a:Root">
        <xsl:variable name="super" select="a:SuperType[1]"/>
        <xsl:variable name="hasMrid" select="cimtool:has-mrid-ancestor(.)"/>
        <item></item>
        <xsl:call-template name="annotate"/>
        <item>@Entity</item>
        <xsl:choose>
            <!-- ========== Case 1: IdentifiedObject hierarchy ========== -->
            <!-- 
				@Table with @Index for FK columns. No uniqueConstraints needed —
				the natural mRID PK is the authoritative unique identifier. 
			-->
            <xsl:when test="$hasMrid">
                <xsl:variable name="className" select="string(@name)"/>
                <xsl:variable name="idxCols"   select="cimtool:index-columns(.)"/>
                <xsl:choose>
                    <xsl:when test="exists($idxCols)">
                        <item>@Table(name="<xsl:value-of select="$className"/>", indexes = {{ <xsl:for-each select="$idxCols">@Index(name="ix_<xsl:value-of select="$className"/>_<xsl:value-of select="."/>", columnList="<xsl:value-of select="."/>")<xsl:if test="position() != last()">, </xsl:if></xsl:for-each> }})</item>
                    </xsl:when>
                    <xsl:otherwise>
                        <item>@Table(name="<xsl:value-of select="$className"/>")</item>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="not($super)">
                    <!-- Root of the IdentifiedObject hierarchy — owns the mRID @Id -->
                    <item>@Inheritance(strategy=InheritanceType.JOINED)</item>
                </xsl:if>
                <xsl:if test="$super">
                    <!-- Subclass joins on mRID -->
                    <item>@PrimaryKeyJoinColumn(name="mRID")</item>
                </xsl:if>
                <list begin="public static class {if ($super) then concat(@name, ' extends ', $super/@name) else @name} {{" indent="    " end="}}">
					<item></item>
					<xsl:if test="not($super)">
                        <!-- Declare mRID @Id only at the root — subclasses inherit it -->
                        <xsl:call-template name="property">
                            <xsl:with-param name="name">mRID</xsl:with-param>
                            <xsl:with-param name="type" select="$mridType"/>
                            <xsl:with-param name="annotate">
                                <item>@Id</item>
                                <item>@Column(name="mRID")</item>
                            </xsl:with-param>
                        </xsl:call-template>
                        <!-- equals() and hashCode() — generated once at the hierarchy
                             root and inherited by all subclasses. Uses getClass() rather
                             than instanceof to correctly handle Hibernate proxy objects.
                             hashCode() returns a fixed class-based value so it remains
                             stable across the transient-to-persistent lifecycle. -->
                        <item></item>
                        <item>@Override</item> 
						<list begin="public boolean equals(Object o) {{" indent="    " end="}}">
							<item>if (this == o) return true;</item>
							<item>if (o == null || getClass() != o.getClass()) return false;</item>
							<item>IdentifiedObject that = (IdentifiedObject) o;</item>
							<item>return mRID != null &amp;&amp; mRID.equals(that.mRID);</item>
						</list>
                        <item></item>
                        <item>@Override</item> 
						<list begin="public int hashCode() {{" indent="    " end="}}">
							<item>return getClass().hashCode();</item>
						</list>
                    </xsl:if>
                    <xsl:apply-templates/>
                </list>
            </xsl:when>

            <!-- ========== Case 2: Non-IdentifiedObject — surrogate UUID @Id ========== -->
            <!-- 
				@Table includes a heuristic @UniqueConstraint spanning all required
				non-surrogate columns. This mirrors the companion sql-rdfs-ansi92
				builder's ALTER TABLE ... ADD UNIQUE (...) for these classes.
				The constraint is omitted when no required non-surrogate columns exist
				(cimtool:unique-columns returns an empty sequence). 
			-->
            <xsl:otherwise>
                <xsl:variable name="className"  select="string(@name)"/>
                <xsl:variable name="uniqueCols" select="cimtool:unique-columns(.)"/>
                <xsl:variable name="idxCols"    select="cimtool:index-columns(.)"/>
                <xsl:choose>
                    <xsl:when test="exists($uniqueCols) and exists($idxCols)">
                        <item>@Table(name="<xsl:value-of select="$className"/>", uniqueConstraints = {{ @UniqueConstraint(columnNames = {{<xsl:for-each select="$uniqueCols">"<xsl:value-of select="."/>"<xsl:if test="position() != last()">, </xsl:if></xsl:for-each>}}) }}, indexes = {{ <xsl:for-each select="$idxCols">@Index(name="ix_<xsl:value-of select="$className"/>_<xsl:value-of select="."/>", columnList="<xsl:value-of select="."/>")<xsl:if test="position() != last()">, </xsl:if></xsl:for-each> }})</item>
                    </xsl:when>
                    <xsl:when test="exists($uniqueCols)">
                        <item>@Table(name="<xsl:value-of select="$className"/>", uniqueConstraints = {{ @UniqueConstraint(columnNames = {{<xsl:for-each select="$uniqueCols">"<xsl:value-of select="."/>"<xsl:if test="position() != last()">, </xsl:if></xsl:for-each>}}) }})</item>
                    </xsl:when>
                    <xsl:when test="exists($idxCols)">
                        <item>@Table(name="<xsl:value-of select="$className"/>", indexes = {{ <xsl:for-each select="$idxCols">@Index(name="ix_<xsl:value-of select="$className"/>_<xsl:value-of select="."/>", columnList="<xsl:value-of select="."/>")<xsl:if test="position() != last()">, </xsl:if></xsl:for-each> }})</item>
                    </xsl:when>
                    <xsl:otherwise>
                        <item>@Table(name="<xsl:value-of select="$className"/>")</item>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="not($super) and exists(root(.)//(a:Root|a:ComplexType)[a:SuperType/@baseClass = current()/@baseClass])">
                    <!-- Root of a non-IdentifiedObject hierarchy that has subclasses
                         in this profile — owns the UUID @Id and requires JOINED
                         inheritance. Omitted for leaf classes with no subclasses
                         (e.g. CurveData, RegularTimePoint) where @Inheritance adds
                         no value and misleads the JPA provider. -->
                    <item>@Inheritance(strategy=InheritanceType.JOINED)</item>
                </xsl:if>
                <xsl:if test="$super">
                    <!-- Non-IdentifiedObject subclass — joins on surrogate id column -->
                    <item>@PrimaryKeyJoinColumn(name="id")</item>
                </xsl:if>
                <list begin="public static class {if ($super) then concat(@name, ' extends ', $super/@name) else @name} {{" indent="    " end="}}">
                    <xsl:if test="not($super)">
                        <!-- Declare UUID surrogate @Id only at the root -->
                        <item>@Id</item>
                        <item>@GeneratedValue(strategy=GenerationType.UUID)</item>
                        <item>@Column(name="id")</item>
                        <item>private UUID id;</item>
                        <item></item>
                        <list begin="public UUID getId() {{" indent="    " end="}}">
							<item>return id;</item>
						</list>
						<list begin="public void setId(UUID id) {{" indent="    " end="}}">
							<item>this.id = id;</item>
						</list>
                        <item></item>
                        <!-- equals() and hashCode() — generated once at the hierarchy
                             root and inherited by all subclasses. Uses getClass() to
                             correctly handle Hibernate proxy objects. hashCode() returns
                             a fixed class-based value for lifecycle stability. -->
                        <item></item>
                        <item>@Override</item> 
						<list begin="public boolean equals(Object o) {{" indent="    " end="}}">
							<item>if (this == o) return true;</item>
							<item>if (o == null || getClass() != o.getClass()) return false;</item>
							<item><xsl:value-of select="$className"/> that = (<xsl:value-of select="$className"/>) o;</item>
							<item>return id != null &amp;&amp; id.equals(that.id);</item>
						</list>
                        <item></item>
                        <item>@Override</item> 
						<list begin="public int hashCode() {{" indent="    " end="}}">
							<item>return getClass().hashCode();</item>
						</list>
                    </xsl:if>
                    <xsl:apply-templates/>
                </list>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- EnumeratedType class template                                       -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template match="a:EnumeratedType">
        <item></item>
        <xsl:call-template name="annotate"/>
        <item>@Entity</item>
        <item>@Table(name="<xsl:value-of select="@name"/>")</item>
        <list begin="public static class {@name} {{" indent="    " end="}};">
            <xsl:call-template name="property">
                <xsl:with-param name="name">name</xsl:with-param>
                <xsl:with-param name="type">String</xsl:with-param>
                <xsl:with-param name="annotate">
                    <item>@Id</item>
                    <item>@Column(name="name")</item>
                </xsl:with-param>
            </xsl:call-template>
            <item></item> 
            <item>@Override</item> 
			<list begin="public boolean equals(Object o) {{" indent="    " end="}}">
				<item>if (this == o) return true;</item>
				<item>if (o == null || getClass() != o.getClass()) return false;</item>
				<item><xsl:value-of select="@name"/> that = (<xsl:value-of select="@name"/>) o;</item>
				<item>return name != null &amp;&amp; name.equals(that.name);</item>
			</list>
            <item></item>
            <item>@Override</item> 
			<list begin="public int hashCode() {{" indent="    " end="}}">
				<item>return getClass().hashCode();</item>
			</list>
        </list>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- CompoundType class template                                          -->
    <!--                                                                      -->
    <!-- CIM Compound types are value objects with no independent identity.   -->
    <!-- Each compound row has exactly one owner and is never shared between  -->
    <!-- parent columns or parent rows — see the companion sql-rdfs-ansi92    -->
    <!-- DDL script header for full design documentation.                     -->
    <!--                                                                      -->
    <!-- JPA mapping:                                                         -->
    <!--   - @Entity + @Table: maps to its own table, same as non-IO classes  -->
    <!--   - UUID @Id via @GeneratedValue(strategy=UUID): auto-assigned by    -->
    <!--     the JPA provider on persist — no constructor UUID assignment     -->
    <!--     needed (unlike C# where Guid.NewGuid() must be called manually)  -->
    <!--   - No @Inheritance: compound types are standalone value objects     -->
    <!--     and do not participate in inheritance hierarchies                -->
    <!--   - Nested a:Compound references use CascadeType.REMOVE so that      -->
    <!--     nested compound children are deleted with their parent           -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template match="a:CompoundType">
        <xsl:variable name="className" select="string(@name)"/>
        <xsl:variable name="idxCols"   select="cimtool:index-columns(.)"/>
        <item></item>
        <xsl:call-template name="annotate"/>
        <item>@Entity</item>
        <xsl:choose>
            <xsl:when test="exists($idxCols)">
                <item>@Table(name="<xsl:value-of select="$className"/>", indexes = {{ <xsl:for-each select="$idxCols">@Index(name="ix_<xsl:value-of select="$className"/>_<xsl:value-of select="."/>", columnList="<xsl:value-of select="."/>")<xsl:if test="position() != last()">, </xsl:if></xsl:for-each> }})</item>
            </xsl:when>
            <xsl:otherwise>
                <item>@Table(name="<xsl:value-of select="$className"/>")</item>
            </xsl:otherwise>
        </xsl:choose>
        <list begin="public static class {@name} {{" indent="    " end="}}">
            <item></item>
            <item>@Id</item>
            <item>@GeneratedValue(strategy=GenerationType.UUID)</item>
            <item>@Column(name="id")</item>
            <item>private UUID id;</item>
            <item></item>
            <list begin="public UUID getId() {{" indent="    " end="}}">
				<item>return id;</item>
            </list>
            <list begin="public void setId(UUID id) {{" indent="    " end="}}">
				<item>this.id = id;</item>
            </list>
            <item></item>
            <item>@Override</item> 
			<list begin="public boolean equals(Object o) {{" indent="    " end="}}">
				<item>if (this == o) return true;</item>
				<item>if (o == null || getClass() != o.getClass()) return false;</item>
				<item><xsl:value-of select="$className"/> that = (<xsl:value-of select="$className"/>) o;</item>
				<item>return id != null &amp;&amp; id.equals(that.id);</item>
			</list>
            <item></item>
            <item>@Override</item> 
			<list begin="public int hashCode() {{" indent="    " end="}}">
				<item>return getClass().hashCode();</item>
			</list>
            <xsl:apply-templates/>
        </list>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- Single-valued association (a:Instance, a:Reference, a:Compound)    -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template match="a:Instance|a:Reference|a:Compound">
        <xsl:choose>
            <xsl:when test="not(@maxOccurs) or @maxOccurs = '1'">
                <!-- Single-valued: @ManyToOne with optional=false when required -->
                <xsl:if test="@name != 'mRID'">
                    <decorate>
                        <xsl:call-template name="annotate"/>
                        <xsl:call-template name="property">
                            <xsl:with-param name="name" select="@name"/>
                            <xsl:with-param name="type" select="@type"/>
                            <xsl:with-param name="annotate">
                                <item>
                                    <list begin="@ManyToOne(" delim=", " end=")">
                                        <item>fetch=FetchType.LAZY</item>
                                        <xsl:if test="self::a:Compound">
                                            <!-- CascadeType.REMOVE implements the compound value
                                                 object ownership model: when a parent entity is
                                                 deleted, its compound child is automatically
                                                 deleted by the JPA provider. This mirrors the
                                                 ON DELETE CASCADE in the companion SQL DDL. -->
                                            <item>cascade=CascadeType.REMOVE</item>
                                        </xsl:if>
                                        <xsl:if test="@minOccurs = '1'">
                                            <item>optional=false</item>
                                        </xsl:if>
                                    </list>
                                </item>
                                <item>@JoinColumn(name="<xsl:value-of select="@name"/>")</item>
                            </xsl:with-param>
                        </xsl:call-template>
                    </decorate>
                </xsl:if>
            </xsl:when>
            <!-- Unbounded collection (maxOccurs > '1' or 'unbounded'): @OneToMany
                 intentionally suppressed. See "No @OneToMany Mappings — Intentional
                 Design" in the file header for the full rationale. A self-documenting
                 comment block is emitted in the generated source in place of the
                 suppressed mapping so that implementers know the association exists
                 and are shown the correct JPQL pattern to use instead. -->
            <xsl:otherwise>
                <xsl:variable name="childType"      select="string(@type)"/>
                <xsl:variable name="assocName"      select="cimtool:capitalize(@name, '')"/>
                <xsl:variable name="parentType"     select="string(parent::*/@name)"/>
                <xsl:variable name="inversePropRaw"
                    select="if (@inverseBaseProperty)
                            then tokenize(string(@inverseBaseProperty), '[\.#]')[last()]
                            else ''"/>
                <xsl:variable name="inverseField"   select="if ($inversePropRaw != '') then cimtool:uncapitalize($inversePropRaw) else ''"/>
                <xsl:variable name="orderByField"
                    select="if (cimtool:has-mrid-ancestor(parent::*)) then 'mRID' else 'id'"/>
                <item></item>
                <list begin="/**" indent="* " end="*/">
					<item>─────────────────────────────────────────────────────────────────────────────────────────</item>
					<item>Suppressed @OneToMany:  <xsl:value-of select="$parentType"/> → <xsl:value-of select="$childType"/>  [<xsl:value-of select="@minOccurs"/>..*]</item>
					<item>The profile declares a [<xsl:value-of select="@minOccurs"/>..*] association on this class to <xsl:value-of select="$childType"/>.</item>
					<item></item>
					<item>A @OneToMany collection mapping has been intentionally suppressed:</item>
					<item></item>
					<list begin="" indent="    " end="">
						<item>// NOT generated</item>
						<item>@OneToMany(mappedBy="<xsl:value-of select="$inverseField"/>", fetch=FetchType.LAZY)</item>
						<item>private List&lt;<xsl:value-of select="$childType"/>&gt; <xsl:value-of select="cimtool:uncapitalize($assocName)"/>;</item>
					</list>
					<item></item>
					<item>See "No @OneToMany Mappings — Intentional Design" in the file header for</item>
					<item>the full rationale. In summary: unbounded collections risk loading entire</item>
					<item>result sets into memory with no pagination, require bidirectional sync on</item>
					<item>every add/remove, and are not consistently present across profiles.</item>
					<item>Use an explicit JPQL query through the child side instead:</item>
					<item></item>
					<item>Basic traversal:</item>
					<list begin="" indent="    " end="">
						<item>List&lt;<xsl:value-of select="$childType"/>&gt; results = em.createQuery(</item>
						<list begin="" indent="    " end="">
							<item>"SELECT x FROM <xsl:value-of select="$childType"/> x " +</item>
							<item>"WHERE x.<xsl:value-of select="$inverseField"/> = :parent",</item>
							<item><xsl:value-of select="$childType"/>.class)</item>
							<item>.setParameter("parent", this)</item>
							<item>.getResultList();</item>
						</list>
					</list>
					<item></item>
					<item>Paginated traversal:</item>
					<list begin="" indent="    " end="">
						<item>List&lt;<xsl:value-of select="$childType"/>&gt; page = em.createQuery(</item>
						<list begin="" indent="    " end="">
							<item>"SELECT x FROM <xsl:value-of select="$childType"/> x " + </item>
							<item>"WHERE x.<xsl:value-of select="$inverseField"/> = :parent ORDER BY x.<xsl:value-of select="$orderByField"/>",</item>
							<item><xsl:value-of select="$childType"/>.class)</item>
							<item>.setParameter("parent", this)</item>
							<item>.setFirstResult(offset)</item>
							<item>.setMaxResults(pageSize)</item>
							<item>.getResultList();</item>
						</list>
					</list>
					<item>─────────────────────────────────────────────────────────────────────────────────────────</item>
				</list>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- Enumerated attribute — stored as String FK to enum table            -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template match="a:Enumerated">
        <decorate>
            <xsl:call-template name="annotate"/>
            <xsl:call-template name="property">
                <xsl:with-param name="name" select="@name"/>
                <xsl:with-param name="type">String</xsl:with-param>
                <xsl:with-param name="annotate">
                    <xsl:choose>
                        <xsl:when test="@minOccurs = '1'">
                            <item>@Column(name="<xsl:value-of select="@name"/>", nullable=false)</item>
                        </xsl:when>
                        <xsl:otherwise>
                            <item>@Column(name="<xsl:value-of select="@name"/>")</item>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:with-param>
            </xsl:call-template>
        </decorate>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- Simple attribute and Domain (CIM datatype scalar)                    -->
    <!--                                                                      -->
    <!-- Boolean column definition:                                           -->
    <!--   CIM boolean attributes map to Boolean in Java. Without a           -->
    <!--   columnDefinition hint most JPA providers will create a             -->
    <!--   BOOLEAN or BIT column, which does not match the companion          -->
    <!--   sql-rdfs-ansi92 builder's INTEGER DEFAULT 0 CHECK (...) pattern.   -->
    <!--   columnDefinition="INTEGER DEFAULT 0" is therefore emitted for      -->
    <!--   all boolean attributes to align the JPA schema with the SQL DDL.   -->
    <!--   nullable=false is still appended when minOccurs=1, matching the    -->
    <!--   NOT NULL emitted by the SQL DDL builder for required booleans.     -->
    <!--                                                                      -->
    <!-- Oracle portability note:                                             -->
    <!--   columnDefinition="INTEGER DEFAULT 0" is passed verbatim to the    -->
    <!--   RDBMS during JPA schema generation. On Oracle, INTEGER maps to     -->
    <!--   NUMBER(38) rather than the idiomatic NUMBER(1). If targeting       -->
    <!--   Oracle and using JPA schema generation, override columnDefinition  -->
    <!--   to NUMBER(1) DEFAULT 0 or use a custom AttributeConverter.        -->
    <!--   This does not apply when using the companion sql-rdfs-ansi92 DDL  -->
    <!--   script for schema creation (the recommended approach).             -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template match="a:Simple|a:Domain">
        <xsl:if test="(not(@maxOccurs) or @maxOccurs = '1') and @name != 'mRID'">
            <decorate>
				<item></item>
                <xsl:call-template name="annotate"/>
                <xsl:call-template name="property">
                    <xsl:with-param name="name" select="@name"/>
                    <xsl:with-param name="xstype" select="@xstype"/>
                    <xsl:with-param name="annotate">
                        <xsl:choose>
                            <!-- Boolean: emit columnDefinition to match SQL DDL's
                                 INTEGER DEFAULT 0 CHECK ("col" IN (0, 1)) pattern.
                                 nullable=false is appended when minOccurs=1. -->
                            <xsl:when test="@xstype = 'boolean' and @minOccurs = '1'">
                                <!-- Oracle note: INTEGER maps to NUMBER(38) on Oracle. If targeting
                                     Oracle with JPA schema generation, change to NUMBER(1) DEFAULT 0.
                                     Not applicable when using the companion SQL DDL script directly. -->
                                <item>@Column(name="<xsl:value-of select="@name"/>", columnDefinition="INTEGER DEFAULT 0", nullable=false)</item>
                            </xsl:when>
                            <xsl:when test="@xstype = 'boolean'">
                                <!-- Oracle note: INTEGER maps to NUMBER(38) on Oracle. If targeting
                                     Oracle with JPA schema generation, change to NUMBER(1) DEFAULT 0.
                                     Not applicable when using the companion SQL DDL script directly. -->
                                <item>@Column(name="<xsl:value-of select="@name"/>", columnDefinition="INTEGER DEFAULT 0")</item>
                            </xsl:when>
                            <!-- Non-boolean required attribute -->
                            <xsl:when test="@minOccurs = '1'">
                                <item>@Column(name="<xsl:value-of select="@name"/>", nullable=false)</item>
                            </xsl:when>
                            <!-- Non-boolean optional attribute -->
                            <xsl:otherwise>
                                <item>@Column(name="<xsl:value-of select="@name"/>")</item>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:with-param>
                </xsl:call-template>
            </decorate>
        </xsl:if>
    </xsl:template>

    <!-- ════════════════════════════════════════════════════════════════════ -->
    <!-- Javadoc comment generation                                          -->
    <!-- ════════════════════════════════════════════════════════════════════ -->
    <xsl:template name="annotate">
        <list begin="/**" indent=" * " end=" */">
            <xsl:apply-templates mode="annotate"/>
        </list>
    </xsl:template>

    <xsl:template match="a:Comment|a:Note" mode="annotate">
        <wrap width="70">
            <xsl:value-of select="."/>
        </wrap>
    </xsl:template>

    <xsl:template match="text()"/>

    <xsl:template match="node()" mode="config"/>

    <xsl:template match="node()" mode="annotate"/>

</xsl:stylesheet>
