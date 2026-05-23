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

    <!--
       Revised CIMTOOL C# Class Generator with Properties
       This XSLT generates class declarations (names) along with:
         - A default (parameterless) constructor
         - A ToString() override
         - Auto-implemented properties for attributes and associations
       Licensed under the Apache License, Version 2.0.
    -->

    <xsl:output indent="yes" method="xml" encoding="UTF-8" />

    <!-- Parameters -->
    <xsl:param name="version"/>
    <xsl:param name="baseURI"/>
    <xsl:param name="envelope">Profile</xsl:param>
    <xsl:param name="package">io.ucaiug.cimtool.generated</xsl:param>
    <xsl:param name="mridType">string</xsl:param>

    <!--
    ════════════════════════════════════════════════════════════════════════════════
    BEGIN: TYPE MAPPING FUNCTIONS
    ════════════════════════════════════════════════════════════════════════════════
    -->

    <!--
        Returns a C# [MaxLength(n)] annotation string for string-like XSD types,
        or an empty sequence for types that carry no MaxLength constraint in EF Core.

        These type lengths should strictly correlate to the length values as
        generated in the SQL DDL script by the sql.xsl builder.

        Parameters:
            $xstype  — The XSD simple type name taken from @xstype on the profile
                       attribute node (e.g. 'string', 'integer', 'dateTime').
            $name    — The attribute or column name taken from @name on the profile
                       attribute node. Used to distinguish the 'mRID' and surrogate
                       compound 'id' columns, which are capped at 100 rather than 255.

        Returns:
            xs:string?  — One of:
                '[MaxLength(100)]'   for mRID and compound surrogate id columns
                '[MaxLength(255)]'   for string, normalizedString, and token types
                '[MaxLength(2048)]'  for anyURI (per practical browser compatibility ceiling)
                ()                   for all non-string types (numeric, boolean, binary,
                                     date/time) which carry no MaxLength annotation in C#

        Notes:
            - The empty sequence return allows the call site to use exists() as a
              clean gate, or simply pass the result to xsl:value-of which produces
              no output for an empty sequence.
            - BLOB types (base64Binary, hexBinary) intentionally return () — EF Core
              maps these to byte[] which carries no MaxLength constraint by default.
            - The boolean type maps to INTEGER 0/1 in SQL and to bool in C#, neither
              of which uses MaxLength.
    -->
    <xsl:function name="cimtool:maxLength" as="xs:string?">
        <xsl:param name="xstype" as="xs:string"/>
        <xsl:param name="name"   as="xs:string"/>

        <xsl:choose>
            <!-- mRID and compound surrogate id are always capped at 100 -->
            <xsl:when test="($xstype = 'string') and ($name = 'mRID' or $name = 'id')">
                <xsl:sequence select="'[MaxLength(100)]'"/>
            </xsl:when>

            <!-- Standard string-like types -->
            <xsl:when test="$xstype = 'string' or $xstype = 'normalizedString' or $xstype = 'token'">
                <xsl:sequence select="'[MaxLength(255)]'"/>
            </xsl:when>

            <!-- URLs — 2048 per practical browser/IE compatibility ceiling -->
            <xsl:when test="$xstype = 'anyURI'">
                <xsl:sequence select="'[MaxLength(2048)]'"/>
            </xsl:when>

            <!-- Non-string types carry no MaxLength annotation:        -->
            <!-- short → SMALLINT                                        -->
            <!-- int/integer → INTEGER                                   -->
            <!-- long → BIGINT                                           -->
            <!-- decimal/float/double → DOUBLE PRECISION                 -->
            <!-- base64Binary/hexBinary → BLOB                           -->
            <!-- date → DATE                                             -->
            <!-- time → TIME                                             -->
            <!-- dateTime → TIMESTAMP                                    -->
            <!-- boolean → INTEGER 0/1                                   -->
            <xsl:otherwise>
                <xsl:sequence select="()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        Returns the C# primitive or framework type name corresponding to a given
        XSD simple type, for use in generating EF Core entity property declarations.

        Parameters:
            $xstype  — The XSD simple type name taken from @xstype on the profile
                       attribute node (e.g. 'string', 'integer', 'dateTime').

        Returns:
            xs:string  — The C# type name. One of:
                           'string'    for string, normalizedString, token, anyURI
                           'short'     for short
                           'int'       for integer, int
                           'long'      for long
                           'double'    for decimal, float, double
                           'byte[]'    for base64Binary, hexBinary
                           'DateOnly'  for date
                           'TimeOnly'  for time
                           'DateTime'  for dateTime
                           'bool'      for boolean
                           'string'    for all unrecognised types (safe fallback)

        Notes:
            - decimal and float are both mapped to 'double' rather than 'decimal'
              to align with the DOUBLE PRECISION mapping used in the parallel SQL
              DDL generator. If exact decimal arithmetic is required for a specific
              profile, the caller should override accordingly.
            - DateOnly and TimeOnly require .NET 6 or later. If earlier .NET
              versions must be supported, these should be mapped to DateTime.
            - byte[] carries no MaxLength annotation — cimtool:maxLength() correctly
              returns () for base64Binary and hexBinary types.
            - anyURI is mapped to string as there is no native C# Uri property
              type that EF Core maps cleanly across all five target RDBMS backends.
            - Unrecognised types fall back to string, matching the behaviour of
              the parallel SQL DDL generator which falls back to VARCHAR(255).
    -->
    <xsl:function name="cimtool:csType" as="xs:string">
        <xsl:param name="xstype" as="xs:string"/>

        <xsl:choose>
            <!-- String-like types -->
            <xsl:when test="$xstype = 'string' or $xstype = 'normalizedString' or $xstype = 'token' or $xstype = 'anyURI'">
                <xsl:sequence select="'string'"/>
            </xsl:when>

            <!-- Integer types -->
            <xsl:when test="$xstype = 'short'">
                <xsl:sequence select="'short'"/>
            </xsl:when>
            <xsl:when test="$xstype = 'integer' or $xstype = 'int'">
                <xsl:sequence select="'int'"/>
            </xsl:when>
            <xsl:when test="$xstype = 'long'">
                <xsl:sequence select="'long'"/>
            </xsl:when>

            <!-- Floating point types — all map to double to align with
                 DOUBLE PRECISION in the parallel SQL DDL generator -->
            <xsl:when test="$xstype = 'decimal' or $xstype = 'float' or $xstype = 'double'">
                <xsl:sequence select="'double'"/>
            </xsl:when>

            <!-- Binary types -->
            <xsl:when test="$xstype = 'base64Binary' or $xstype = 'hexBinary'">
                <xsl:sequence select="'byte[]'"/>
            </xsl:when>

            <!-- Date and time types — require .NET 6+ -->
            <xsl:when test="$xstype = 'date'">
                <xsl:sequence select="'DateOnly'"/>
            </xsl:when>
            <xsl:when test="$xstype = 'time'">
                <xsl:sequence select="'TimeOnly'"/>
            </xsl:when>
            <xsl:when test="$xstype = 'dateTime'">
                <xsl:sequence select="'DateTime'"/>
            </xsl:when>

            <!-- Boolean — maps to INTEGER 0/1 in SQL, bool in C# -->
            <xsl:when test="$xstype = 'boolean'">
                <xsl:sequence select="'bool'"/>
            </xsl:when>

            <!-- Safe fallback — mirrors VARCHAR(255) fallback in SQL DDL generator -->
            <xsl:otherwise>
                <xsl:sequence select="'string'"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
        Returns a capitalized version of the given name string, with any
        hyphens replaced by underscores prior to capitalisation.

        Parameters:
            $name  — The name string to capitalize, typically taken from @name
                     on a profile element node.

        Returns:
            xs:string  — The input string with hyphens replaced by underscores
                         and the first character converted to uppercase.

        Notes:
            - The lowercase and uppercase alphabet variables ($lc, $uc) are
              intentionally scoped inside this function as they are only used
              here. The translate() approach is used in preference to the
              XPath 3.0 upper-case() function to keep capitalisation behaviour
              explicit and locale-independent.
            - Only the first character is capitalized — the remainder of the
              string is preserved exactly as supplied.
            - This function is naturally immune to C# keyword conflicts because
              it always produces a result beginning with an uppercase letter,
              and every C# reserved and contextual keyword begins with a
              lowercase letter. cimtool:safeIdentifier() is therefore not needed
              for any call site that uses this function.
    -->
    <xsl:function name="cimtool:capitalize" as="xs:string">
        <xsl:param name="name" as="xs:string"/>

        <xsl:variable name="lc">abcdefghijklmnopqrstuvwxyz</xsl:variable>
        <xsl:variable name="uc">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
        <xsl:variable name="clean" select="translate($name, '-', '_')"/>

        <!-- Special case: 'mRID' must capitalize to 'MRId' (lowercase 'd'), not
             the generic 'MRID' that the first-char-uppercase rule would produce.
             This matches the IEC CIM convention where the identifier is 'mRID'
             (master Resource IDentifier) with a lowercase terminal 'd'. -->
        <xsl:choose>
            <xsl:when test="$clean = 'mRID'">
                <xsl:sequence select="'MRId'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:sequence select="concat(
                    translate(substring($clean, 1, 1), $lc, $uc),
                    substring($clean, 2)
                )"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
	<!--
	    Returns a safe C# identifier from the given name string by:
	      1. Replacing hyphens with underscores (hyphens are invalid in C# identifiers)
	      2. Prefixing with '_' if the result begins with a digit (C# identifiers
	         cannot start with a digit, e.g. a hypothetical enum value '3phase')
	      3. Prefixing with '@' if the result matches a C# reserved or contextual
	         keyword (e.g. 'base', 'default', 'in', 'value', 'get', 'set')
	
	    This function is intentionally case-preserving — it does NOT capitalize the
	    first character. Use cimtool:capitalize() for PascalCase property names.
	    Use this function directly for enum literal identifiers where case must be
	    preserved to avoid collisions between values that differ only in case
	    (e.g. UnitSymbol 'H' henry vs 'h' hour).
	
	    Parameters:
	        $name  — The raw name string, typically taken from @name on a profile
	                 element node (e.g. an a:EnumeratedValue).
	
	    Returns:
	        xs:string  — A valid C# identifier. Examples:
	                       'H'          → 'H'
	                       'h'          → 'h'
	                       'some-value' → 'some_value'
	                       '3phase'     → '_3phase'
	                       'base'       → '@base'
	                       'default'    → '@default'
	                       'value'      → '@value'
	
	    Notes:
	        - Covers all C# reserved keywords (C# specification section 6.4.4) and
	          all contextual keywords (section 6.4.4.1) since contextual keywords
	          can still conflict in certain syntactic positions.
	        - Digit-leading values are prefixed with '_' rather than '@' since '@'
	          is only valid for keyword escaping in C# — '@3phase' is not legal.
	        - cimtool:capitalize() is naturally safe against both issues since it
	          always produces a PascalCase result beginning with an uppercase letter,
	          and all C# keywords are lowercase. This function is therefore only
	          needed for case-preserving enum literal generation.
	-->
	<xsl:function name="cimtool:safeIdentifier" as="xs:string">
	    <xsl:param name="name" as="xs:string"/>
	
	    <xsl:variable name="clean" select="translate($name, '-', '_')"/>
	
	    <!-- C# reserved keywords — C# specification section 6.4.4 -->
	    <xsl:variable name="reserved" as="xs:string+" select="(
	        'abstract', 'as',       'base',      'bool',      'break',
	        'byte',     'case',     'catch',     'char',      'checked',
	        'class',    'const',    'continue',  'decimal',   'default',
	        'delegate', 'do',       'double',    'else',      'enum',
	        'event',    'explicit', 'extern',    'false',     'finally',
	        'fixed',    'float',    'for',       'foreach',   'goto',
	        'if',       'implicit', 'in',        'int',       'interface',
	        'internal', 'is',       'lock',      'long',      'namespace',
	        'new',      'null',     'object',    'operator',  'out',
	        'override', 'params',   'private',   'protected', 'public',
	        'readonly', 'ref',      'return',    'sbyte',     'sealed',
	        'short',    'sizeof',   'stackalloc','static',    'string',
	        'struct',   'switch',   'this',      'throw',     'true',
	        'try',      'typeof',   'uint',      'ulong',     'unchecked',
	        'unsafe',   'ushort',   'using',     'virtual',   'void',
	        'volatile', 'while'
	    )"/>
	
	    <!-- C# contextual keywords — section 6.4.4.1 -->
	    <xsl:variable name="contextual" as="xs:string+" select="(
	        'add',       'alias',     'ascending', 'async',     'await',
	        'by',        'descending','dynamic',   'equals',    'from',
	        'get',       'global',    'group',     'into',      'join',
	        'let',       'nameof',    'on',        'orderby',   'partial',
	        'remove',    'select',    'set',       'unmanaged', 'value',
	        'var',       'when',      'where',     'with',      'yield'
	    )"/>
	
	    <xsl:sequence select="
	        if (matches($clean, '^[0-9]'))
	        then concat('_', $clean)
	        else if ($clean = $reserved or $clean = $contextual)
	        then concat('@', $clean)
	        else $clean
	    "/>
	</xsl:function>

    <!--
        Returns a safe C# property name for a given profile attribute, guarding
        against two distinct collision classes that cimtool:capitalize() alone
        cannot detect:

        Collision class 1 — property name matches a sibling class name.
            Example: a:Enumerated @name='curveStyle' on class Curve capitalizes
            to 'CurveStyle', which is also a sibling EnumeratedType class name.
            In a nested-class context, the unqualified name resolves to the type,
            making 'public string CurveStyle' ambiguous or a compile error
            depending on usage. Similarly 'sVCControlMode' → 'SVCControlMode'
            collides with the SVCControlMode EnumeratedType class.

        Collision class 2 — property name matches its own C# type name.
            Example: a:Simple @name='dateTime' @xstype='dateTime' capitalizes to
            'DateTime' and maps to C# type 'DateTime', producing:
                public DateTime? DateTime { get; set; }
            Within the class body, 'DateTime' now resolves to the property rather
            than the System.DateTime type, breaking any unqualified type references.

        In both cases the fix is to append 'Value' to the property name. This is
        the conventional CIM disambiguation suffix (cf. 'value1', 'value2' in
        BasicIntervalSchedule) and does not affect the [Column("...")] annotation,
        which preserves the original database column name unchanged.

        Parameters:
            $name    — The raw attribute name from @name on the profile element.
            $xstype  — The XSD type string from @xstype. Pass the empty string ''
                       for a:Enumerated properties which carry no @xstype.
            $context — Any element node within the current document, used to reach
                       the document root for the catalog class-name lookup.

        Returns:
            xs:string — A collision-free PascalCase C# property name.

        Notes:
            - cimtool:capitalize() is called first, so the mRID → MRId special
              case is already applied before collision checking.
            - Navigation property names produced by a:Instance / a:Reference /
              a:Compound are intentionally NOT routed through this function.
              'public BusNameMarker? BusNameMarker' (property same name as its
              type) is legal C# and is the EF Core idiomatic pattern.
            - Collision detection checks the full catalog, not just the enclosing
              class, because all generated classes share the same outer namespace.
    -->
    <xsl:function name="cimtool:safePropertyName" as="xs:string">
        <xsl:param name="name"    as="xs:string"/>
        <xsl:param name="xstype"  as="xs:string"/>
        <xsl:param name="context" as="element()"/>

        <xsl:variable name="base" select="cimtool:capitalize($name)"/>

        <!-- Collision 1: base name matches any catalog class name -->
        <xsl:variable name="classCollision" as="xs:boolean" select="
            exists(root($context)//(a:EnumeratedType|a:CompoundType|a:ComplexType|a:Root)
                   [@name = $base])
        "/>

        <!-- Collision 2: base name matches the C# type of this attribute -->
        <xsl:variable name="typeCollision" as="xs:boolean" select="
            $xstype != '' and $base = cimtool:csType($xstype)
        "/>

        <xsl:sequence select="
            if ($classCollision or $typeCollision) then concat($base, 'Value') else $base
        "/>
    </xsl:function>

    <!--
        Returns true if the given CIM profile element, or any ancestor reached by
        walking the a:SuperType chain, directly declares an attribute named 'mRID'.
        This is the definitive programmatic test for membership in the
        IdentifiedObject hierarchy within the profile catalog XML.

        BACKGROUND:
        
        In the CIM, IdentifiedObject is the universal root class for all persistent,
        independently-addressable objects. It carries the 'mRID' (master resource
        identifier) attribute. In a CIMTool profile, an entity in this hierarchy 
        will either:
        
          a) declare 'mRID' directly as an a:Simple child (if it IS IdentifiedObject
             or a profile includes mRID explicitly), or
          b) inherit it transitively through the a:SuperType chain.

        A small but important set of CIM classes do NOT inherit from IdentifiedObject.
        These are typically collection-member or point-data classes, for example:
          - CurveData               (owned by Curve)
          - RegularTimePoint        (owned by RegularIntervalSchedule)
          - NonlinearShuntCompensatorPoint  (owned by NonlinearShuntCompensator)
          - TapChangerTablePoint    (base class for PhaseTapChangerTablePoint,
                                     RatioTapChangerTablePoint)

        These classes have no natural single-column primary key derivable from the
        CIM model. The generators handle them with a surrogate 'id' PK (identical
        to the CompoundType pattern) plus a heuristic UNIQUE constraint across all
        required non-surrogate columns as a guard against semantically corrupt
        duplicate rows. See the a:ComplexType|a:Root template for the full
        implementation.

        This function is structurally similar to cimtool:inherits-from but checks
        for the presence of an mRID attribute rather than a specific @baseClass URI.
        A dedicated function is used rather than extending cimtool:inherits-from so
        that each function remains single-purpose and its intent is unambiguous at
        every call site.

        Parameters:
            $element  — The a:ComplexType or a:Root element to examine.

        Returns:
            xs:boolean  — true if mRID is present at this class or any ancestor.
    -->
    <xsl:function name="cimtool:has-mrid-ancestor" as="xs:boolean">
        <xsl:param name="element" as="element()"/>
        <xsl:sequence select="
            if ($element/a:Simple[@name='mRID']) then
                true()
            else if ($element/a:SuperType) then
                let $superBaseClass := string($element/a:SuperType[1]/@baseClass),
                    $parent := (
                        root($element)//a:Root     [@baseClass = $superBaseClass] |
                        root($element)//a:ComplexType[@baseClass = $superBaseClass]
                    )[1]
                return
                    if ($parent) then cimtool:has-mrid-ancestor($parent)
                    else false()
            else
                false()
        "/>
    </xsl:function>

    <!--
    ════════════════════════════════════════════════════════════════════════════════   
    BEGIN: TOPOLOGICAL SORT FUNCTIONS
    
    These six functions form a self-contained unit with no dependencies on any. 
    They operate purely on the CIMTool profile catalog XML structure (a:Catalog, 
    a:CompoundType, a:Root, a:ComplexType, a:SuperType, a:Instance, a:Reference, 
    a:Compound) and the XPath 3.1 map: namespace.

    Note: a:Compound children (compound-type references such as electronicAddress,
    phone1, status, streetDetail) are handled by cimtool:get-dependencies via its
    non-Instance/Reference branch - any child element with @baseClass that is not
    a:SuperType, a:Instance, a:Reference, a:InverseInstance, or a:InverseReference
    is treated as a direct dependency. This correctly ensures that e.g. StreetAddress
    is sorted after Status, StreetDetail, and TownDetail.

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
        Purpose: Checks if an element inherits from a given baseClass by
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

        <xsl:variable name="result" select="
            if ($element/@baseClass = $targetBaseClass) then
                true()
            else if ($element/a:SuperType) then
                let $superBaseClass := $element/a:SuperType/@baseClass,
                    $parent := (
                        root($element)//a:Root[@baseClass = $superBaseClass] |
                        root($element)//a:ComplexType[@baseClass = $superBaseClass]
                    )[1]
                return
                    if ($parent)
                    then cimtool:inherits-from($parent, $targetBaseClass)
                    else false()
            else
                false()
        "/>

        <xsl:sequence select="$result"/>
    </xsl:function>

    <!--
        Function: cimtool:get-union-dependencies
        Purpose: Returns the @baseClass values of all concrete a:Root elements
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

        <xsl:variable name="abstract-baseClass" select="string($abstract-element/@baseClass)"/>

        <xsl:variable name="result" select="
            distinct-values(
                for $root in root($abstract-element)//a:Root
                return
                    if (cimtool:inherits-from($root, $abstract-baseClass))
                    then string($root/@baseClass)
                    else ()
            )
        "/>

        <xsl:sequence select="$result"/>
    </xsl:function>

    <!--
        Function: cimtool:get-dependencies
        Purpose: Returns all distinct @baseClass dependency values for a single
                 element, including dependencies inherited through its a:SuperType
                 chain AND all FK associations (a:Instance, a:Reference). For
                 a:Instance/a:Reference pointing at abstract classes (a:ComplexType),
                 resolves to concrete subclass baseClasses via
                 cimtool:get-union-dependencies. Excludes types present in the
                 $exclusion-map (i.e. already-processed categories).

                 USAGE IN THIS BUILDER — COMPOUND TYPES ONLY:
                 This function is used only for Tier 2 (CompoundType) ordering,
                 where full FK dep tracking is appropriate. CompoundType association
                 networks are shallow and do not create cycles, so the full dep graph
                 produces correct ordering for compound class declarations.

                 For Tier 3 (ComplexType + Root entity class ordering), only
                 inheritance order is needed. C# allows forward class references
                 within a single compilation unit, so FK associations do NOT impose
                 any ordering constraint on class declarations. Using full FK deps
                 for entity classes creates cycles in profiles with dense association
                 graphs (e.g. CGMES CoreEquipment), causing Kahn's algorithm to stall
                 and fall back to document order for 40+ classes.
                 cimtool:get-inheritance-deps is used for Tier 3 instead.
        Parameters:
            $element       — The element to examine (a:CompoundType, a:Root, etc.)
            $exclusion-map — map(xs:string, xs:boolean) of baseClass values to exclude
        Returns:
            xs:string* — Distinct filtered sequence of dependency @baseClass values
    -->
    <xsl:function name="cimtool:get-dependencies" as="xs:string*">
        <xsl:param name="element"       as="element()"/>
        <xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>

        <xsl:variable name="result" select="
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
                   The [1] predicate on $referenced-element guards against profiles where
                   the same @baseClass URI appears on more than one catalog element (e.g. a
                   class defined both as a Root and as a ComplexType in cross-profile
                   scenarios). cimtool:get-union-dependencies requires a single element
                   argument; without [1] it would throw XPTY0004 on such profiles. :)
                for $assoc in $element/(a:Instance|a:Reference)[@baseClass]
                return
                    let $assoc-baseClass      := string($assoc/@baseClass),
                        $referenced-element   := (root($element)/*/node()[@baseClass = $assoc-baseClass])[1]
                    return
                        if ($referenced-element/self::a:ComplexType) then
                            (
                                (: Union members — concrete Root subclasses of this abstract class :)
                                cimtool:get-union-dependencies($referenced-element)
                                    [not(map:contains($exclusion-map, .))],
                                (: The ComplexType itself — needed when it has no Root subclasses in
                                   this profile (e.g. NameType, NameTypeAuthority in EndDeviceControls).
                                   Without this, edges like Name→NameType are invisible to the sort
                                   and ordering becomes arbitrary. When union members do exist,
                                   distinct-values() collapses any redundancy. :)
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

                (: Include parent class itself and its dependencies via SuperType.
                   The parent's own @baseClass must be emitted as a direct dependency
                   so the topological sort places the parent before this element even
                   when the parent has no associations of its own (e.g. WorkLocation
                   before ServiceLocation). Without this, only the parent's transitive
                   dependencies flow through — the parent→child edge is missing. :)
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

        <xsl:sequence select="$result"/>
    </xsl:function>

    <!--
        Function: cimtool:get-inheritance-deps
        Purpose: Returns the @baseClass dependency value needed for topological
                 sorting of EF Core entity classes. Unlike cimtool:get-dependencies
                 (which also tracks FK associations), this function tracks ONLY the
                 direct SuperType parent dependency.

                 For C# EF Core entity class ordering, only inheritance order is
                 needed. C# allows forward class references within a single
                 compilation unit — EF Core resolves all relationships by type name
                 and reflection, not by declaration order. FK associations do NOT
                 impose any ordering constraint on class declarations.

                 Using the full cimtool:get-dependencies for entity class ordering
                 creates cycles in profiles with dense association graphs (e.g. CGMES
                 CoreEquipment where OperationalLimitSet → Equipment → Terminal →
                 ACDCConverter → ... loops back through the association network).
                 These cycles cause Kahn's algorithm to stall and fall back to
                 document order for 40+ classes, defeating the purpose of the sort.

                 By tracking only the direct SuperType parent, this function
                 guarantees a cycle-free dependency graph and produces correct
                 inheritance-ordered output for all CIM profiles.
        Parameters:
            $element       — The element to examine (a:Root or a:ComplexType)
            $exclusion-map — map(xs:string, xs:boolean) of baseClass values to exclude
                             (already-processed tiers such as EnumeratedType and
                             CompoundType)
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
        Purpose: Builds a map(xs:string, xs:boolean) keyed on @baseClass values
                 for a set of already-processed elements. Passed into
                 cimtool:build-dependencies-map to exclude those types from the
                 dependency graph of the next processing tier.
        Parameters:
            $elements — Elements whose @baseClass values should be excluded
                        (e.g. //a:EnumeratedType after enumerations are processed)
        Returns:
            map(xs:string, xs:boolean) — Key: @baseClass value, Value: true()
    -->
    <xsl:function name="cimtool:create-exclusions-map" as="map(xs:string, xs:boolean)">
        <xsl:param name="elements" as="element()*"/>

        <xsl:variable name="result" select="
            map:merge(
                for $element in $elements[@baseClass]
                return map:entry(string($element/@baseClass), true())
            )
        "/>

        <xsl:sequence select="$result"/>
    </xsl:function>

    <!--
        Function: cimtool:build-dependencies-map
        Purpose: Builds a map(xs:string, xs:string*) representing the full
                 dependency graph for a set of elements. Each entry maps an
                 element's @baseClass to the sequence of @baseClass values it
                 depends on (filtered by $exclusion-map).
        Parameters:
            $elements      — Elements to include in the dependency graph
            $exclusion-map — map(xs:string, xs:boolean) of already-processed
                             types to exclude from all dependency lists
        Returns:
            map(xs:string, xs:string*) — Key: @baseClass, Value: dependency sequence
    -->
    <xsl:function name="cimtool:build-dependencies-map" as="map(xs:string, xs:string*)">
        <xsl:param name="elements"      as="element()*"/>
        <xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>

        <xsl:variable name="result" select="
            map:merge(
                for $type in $elements[@baseClass]
                return map:entry(
                    string($type/@baseClass),
                    cimtool:get-dependencies($type, $exclusion-map)
                )
            )
        "/>

        <xsl:sequence select="$result"/>
    </xsl:function>

    <!--
        Function: cimtool:build-inheritance-map
        Purpose: Variant of cimtool:build-dependencies-map that uses
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

        <xsl:variable name="result" select="
            map:merge(
                for $type in $elements[@baseClass]
                return map:entry(
                    string($type/@baseClass),
                    cimtool:get-inheritance-deps($type, $exclusion-map)
                )
            )
        "/>

        <xsl:sequence select="$result"/>
    </xsl:function>

    <!--
        Function: cimtool:topological-sort
        Purpose: Entry point for topological sort. Sorts a sequence of elements
                 into dependency order so that each element appears only after
                 all elements it depends on. Falls back to document order for
                 any elements involved in circular dependencies.
        Parameters:
            $elements  — Elements to sort (e.g. //a:CompoundType or //a:Root)
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
        Purpose: Recursive Kahn's-algorithm implementation. On each pass, finds
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
                <!-- Base case: all elements sorted -->
                <xsl:sequence select="$sorted"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="sorted-baseClasses" select="
                    for $elem in $sorted return string($elem/@baseClass)
                "/>

                <!-- Elements whose every dependency is already in the sorted set -->
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

    <!-- Top-level Catalog template -->
    <xsl:template match="a:Catalog">

        <!-- ── Topological sort — computed once, reused by both class output   -->
        <!-- ── and allClasses array to ensure both are in the same safe order. -->

        <!-- Tier 1: EnumeratedType — no dependencies, processed first -->

        <!-- Tier 2: CompoundType — exclude already-processed EnumeratedTypes -->
        <xsl:variable name="compound-exclusions"
            select="cimtool:create-exclusions-map(//a:EnumeratedType)"/>
        <xsl:variable name="compound-deps-map"
            select="cimtool:build-dependencies-map(//a:CompoundType, $compound-exclusions)"/>
        <xsl:variable name="sorted-compounds"
            select="cimtool:topological-sort(//a:CompoundType, $compound-deps-map)"/>

        <!-- Tier 3: ComplexType + Root — exclude EnumeratedTypes and CompoundTypes.    -->
        <!-- Uses cimtool:get-inheritance-deps rather than cimtool:get-dependencies   -->
        <!-- because only inheritance order is needed for C# class declarations.      -->
        <!-- See cimtool:get-inheritance-deps documentation for full rationale.       -->
        <xsl:variable name="class-exclusions"
            select="cimtool:create-exclusions-map(//a:EnumeratedType|//a:CompoundType)"/>
        <xsl:variable name="class-deps-map"
            select="cimtool:build-inheritance-map(//a:ComplexType|//a:Root, $class-exclusions)"/>
        <xsl:variable name="sorted-classes"
            select="cimtool:topological-sort(//a:ComplexType|//a:Root, $class-deps-map)"/>

        <document>
			<list begin="// ============================================================" indent="// " end="// ============================================================">
				<item>Annotated C# for <xsl:value-of select="$envelope"/></item>
				<item>Generated by CIMTool https://cimtool.ucaiug.io [cimtool.ucaiug.io]</item>
				<item/>
				<item>DO NOT EDIT — this file is fully regenerated by CIMTool on</item>
				<item>every build.  Hand-written customisations belong in partial</item>
				<item>class files alongside this one.</item>
            </list>
            <item/>
            <item>using System;</item>
            <item>using System.Collections.Generic;</item>
            <item>using System.ComponentModel.DataAnnotations;</item>
            <item>using System.ComponentModel.DataAnnotations.Schema;</item>
            <item>using System.Linq.Expressions;</item>
            <item>using System.Threading.Tasks;</item>
            <item>using Microsoft.EntityFrameworkCore;</item>
            <item>using Microsoft.EntityFrameworkCore.ChangeTracking;</item>
            <item/>
            <list begin="" indent="/// " end="">
				<list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
					<item>Entity Framework Core entity classes generated from the</item>
					<item>&lt;b&gt;CoreEquipment&lt;/b&gt; CIMTool profile.</item>
				</list>
				<list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
					<list begin="&lt;para&gt;" indent="" end="&lt;/para&gt;">
						<item>&lt;b&gt;No &lt;c&gt;ICollection&amp;lt;T&amp;gt;&lt;/c&gt; Properties — Intentional Design&lt;/b&gt;&lt;br/&gt;</item>
						<item>Collection-valued inverse navigation properties</item>
						<item>(&lt;c&gt;ICollection&amp;lt;T&amp;gt;&lt;/c&gt;) are intentionally not generated.</item>
						<item>Single-valued foreign-key relationships are fully mapped; inverse</item>
						<item>one-to-many collections are omitted by design, not by omission.</item>
						<item>EF Core does not require them to configure or resolve a relationship.</item>
						<item>They are excluded because:</item>
						<list begin="&lt;list type=&quot;bullet&quot;&gt;" indent="   " end=" &lt;/list&gt;">
							<list begin="&lt;item&gt;" indent="  " end="&lt;/item&gt;">
								<item>CIM associations are unbounded by definition. Materialising an</item>
								<item>unbounded &lt;c&gt;ICollection&amp;lt;T&amp;gt;&lt;/c&gt; into memory with no pagination</item>
								<item>is a runtime hazard for large profiles such as CGMES CoreEquipment.</item>
							</list>
							<list begin="&lt;item&gt;" indent="  " end="&lt;/item&gt;">
								<item>They introduce circular-reference risk during JSON serialisation</item>
								<item>with &lt;c&gt;System.Text.Json&lt;/c&gt; or &lt;c&gt;Newtonsoft.Json&lt;/c&gt; unless</item>
								<item>reference handling is explicitly configured.</item>
							</list>
							<list begin="&lt;item&gt;" indent="  " end="&lt;/item&gt;">
								<item>Inverse associations are profile-dependent and not present in all</item>
								<item>profiles, making generated &lt;c&gt;ICollection&amp;lt;T&amp;gt;&lt;/c&gt; properties</item>
								<item>inconsistent across profiles.</item>
							</list>
							<list begin="&lt;item&gt;" indent="  " end="&lt;/item&gt;">
								<item>Bidirectional EF Core mappings require both sides to be kept</item>
								<item>in sync on every add/remove, adding fragility to consumer code.</item>
							</list>
							<list begin="&lt;item&gt;" indent="  " end="&lt;/item&gt;">
								<item>Explicit LINQ queries support pagination, filtering, and sorting</item>
								<item>that a generated collection property never can.</item>
							</list>
						</list>
						<item>To navigate from a parent entity to its children, query through</item>
						<item>the child side using LINQ:</item>
						<list begin="&lt;code&gt;" indent="" end="&lt;/code&gt;">
							<item>// All children for a given parent (via shadow FK):</item>
							<item>var units = context.Set&lt;PowerElectronicsUnit&gt;()</item>
							<list begin="" indent="    " end="">
								<item>.Where(u => u.PowerElectronicsConnectionId == connection.MRId)</item>
								<item>.ToList();</item>
							</list>
							<item/>
							<item>// With pagination for large result sets:</item>
							<item>var equipment = context.Set&lt;Equipment&gt;()</item>
							<list begin="" indent="    " end="">
								<item>.Where(e => e.EquipmentContainerId == substation.MRId)</item>
								<item>.Skip(0).Take(100).ToList();</item>
							</list>
						</list>
						<item>Consumers who need inverse navigation for specific patterns can add</item>
						<item>&lt;c&gt;ICollection&amp;lt;T&amp;gt;&lt;/c&gt; properties non-destructively via</item>
						<item>&lt;c&gt;partial class&lt;/c&gt; declarations without modifying this generated file.</item>
					</list>	
					<list begin="&lt;para&gt;" indent="" end="&lt;/para&gt;">
						<item>&lt;b&gt;Compound Type References&lt;/b&gt;&lt;br/&gt;</item>
						<item>CIM Compound types are treated as value objects — each compound row has</item>
						<item>exactly one owner and is never shared across parent columns or rows.</item>
						<item>Each compound reference is represented as a shadow FK string property</item>
						<item>(e.g. &lt;c&gt;ElectronicAddressId&lt;/c&gt;) paired with a nullable navigation</item>
						<item>property (e.g. &lt;c&gt;ElectronicAddress?&lt;/c&gt;).</item>
						<item>The companion SQL DDL emits &lt;c&gt;ON DELETE CASCADE&lt;/c&gt; for every compound FK</item>
						<item>(via a reverse-reference pattern that cascades in the correct owner-to-compound</item>
						<item>direction). The generated &lt;c&gt;ModelConfiguration&lt;/c&gt; applies</item>
						<item>&lt;c&gt;DeleteBehavior.Restrict&lt;/c&gt; for compound relationships, enforcing the</item>
						<item>ownership invariant at the EF Core layer: compounds must never be deleted</item>
						<item>directly — only through their parent. The generated &lt;c&gt;DbContextBase&lt;/c&gt;</item>
						<item>&lt;c&gt;SaveChanges&lt;/c&gt; and &lt;c&gt;SaveChangesAsync&lt;/c&gt; overrides detect orphaned</item>
						<item>compound rows after each save and clean them up automatically.</item>
						<item>Each compound entity constructor assigns a new &lt;see cref="System.Guid"/&gt;</item>
						<item>to its &lt;c&gt;Id&lt;/c&gt; property on instantiation, satisfying the strict</item>
						<item>one-compound-per-slot ownership invariant.</item>
					</list>
					<list begin="&lt;para&gt;" indent="" end="&lt;/para&gt;">
						<item>&lt;b&gt;DbSet&amp;lt;T&amp;gt; Scope — Entity Classes Only&lt;/b&gt;&lt;br/&gt;</item>
						<item>&lt;c&gt;DbSet&amp;lt;T&amp;gt;&lt;/c&gt; properties in the generated &lt;c&gt;DbContextBase&lt;/c&gt; are</item>
						<item>limited to persistent CIM entity classes — classes that represent</item>
						<item>independently-addressable objects with their own primary key</item>
						<item>(natural &lt;c&gt;mRID&lt;/c&gt; for IdentifiedObject subclasses, or a surrogate</item>
						<item>&lt;c&gt;id&lt;/c&gt; for non-IdentifiedObject classes such as &lt;c&gt;CurveData&lt;/c&gt;).</item>
						<item>Compound types and enumerated types are intentionally excluded.</item>
						<item>Compounds are value objects owned exclusively by their parent — exposing</item>
						<item>them as first-class &lt;c&gt;DbSet&amp;lt;T&amp;gt;&lt;/c&gt; properties would invite independent</item>
						<item>querying and direct deletion, both of which violate the ownership invariant.</item>
						<item>Enumerated types are referenced only by their string literal values at</item>
						<item>runtime and lookup table rows are never queried in well-written application</item>
						<item>code. Both remain accessible via &lt;c&gt;context.Set&amp;lt;T&amp;gt;()&lt;/c&gt; when</item>
						<item>direct access is genuinely needed.</item>
					</list>
					<list begin="&lt;para&gt;" indent="" end="&lt;/para&gt;">
						<item>&lt;b&gt;DbContext Integration&lt;/b&gt;&lt;br/&gt;</item>
						<item>This file includes a generated &lt;c&gt;DbContextBase&lt;/c&gt; nested abstract class</item>
						<item>containing DbSet properties, complete EF Core Fluent API configuration,</item>
						<item>and compound orphan cleanup logic. Create the following subclass once</item>
						<item>in your project — it will not be overwritten by CIMTool:</item>
						<list begin="&lt;code&gt;" indent="" end="&lt;/code&gt;">
							<item>public class <xsl:value-of select="$envelope"/>DbContext : <xsl:value-of select="$envelope"/>.DbContextBase</item>
							<list begin="{{" indent="    " end="}}">
								<item>public <xsl:value-of select="$envelope"/>DbContext(DbContextOptions&lt;<xsl:value-of select="$envelope"/>DbContext&gt; options)</item>
								<list begin="" indent="    " end="">
									<item>: base(options) { }</item>
								</list>
							</list>
						</list>
					</list>	
				</list>	
            </list>
            <item>public class <xsl:value-of select="$envelope"/></item>
            <list begin="{{" indent="    " end="}}">
                <!-- Classes emitted in topological order -->
                <xsl:apply-templates select="a:EnumeratedType"/>
                <xsl:apply-templates select="$sorted-compounds"/>
                <xsl:apply-templates select="$sorted-classes"/>
                <!-- allClasses array reuses the same sorted sequences -->
                <xsl:call-template name="config">
                    <xsl:with-param name="sorted-compounds" select="$sorted-compounds"/>
                    <xsl:with-param name="sorted-classes"   select="$sorted-classes"/>
                </xsl:call-template>
                <!-- ModelConfiguration and DbConfigBase static classes — complete Fluent API configuration -->
                <xsl:call-template name="dbcontext">
                    <xsl:with-param name="sorted-compounds" select="$sorted-compounds"/>
                    <xsl:with-param name="sorted-classes"   select="$sorted-classes"/>
                </xsl:call-template>		
            </list>
        </document>
    </xsl:template>

    <!--
        Class template for ComplexType and Root.

        ROUTING LOGIC
        ─────────────
        This template handles two structurally distinct cases, detected by
        cimtool:has-mrid-ancestor():

        Case 1 — IdentifiedObject hierarchy (mRID ancestor found)
          The standard Table-Per-Type (TPT) path. The class inherits mRID as its
          natural primary key from IdentifiedObject (directly or transitively). EF
          Core maps the class to its own table, inheriting the [Key] from the root
          entity. A superclass reference is emitted as a C# base class.

        Case 2 — Non-IdentifiedObject class (no mRID ancestor)
          A small subset of CIM classes do not inherit from IdentifiedObject. These
          are typically collection-member or point-data classes (e.g. CurveData,
          RegularTimePoint, NonlinearShuntCompensatorPoint, TapChangerTablePoint).
          They have no natural single-column PK derivable from the CIM model.

          Treatment mirrors the CompoundType surrogate pattern with two sub-cases:

          2a — Has a superclass (e.g. PhaseTapChangerTablePoint, RatioTapChangerTablePoint)
            The entity participates in a non-IdentifiedObject TPT hierarchy. It
            inherits the surrogate 'id' key from its parent and MUST NOT redeclare
            it. The class declaration emits C# inheritance (': SuperType') without
            any [Key] block.

          2b — No superclass (e.g. TapChangerTablePoint, CurveData, RegularTimePoint,
            NonlinearShuntCompensatorPoint)
            A surrogate VARCHAR(100) 'id' column is emitted as the [Key].
            A heuristic [Index(..., IsUnique=true)] is emitted across all required
            (minOccurs=1) non-surrogate scalar properties and single-valued FK
            shadow properties. This is the best uniqueness guard achievable without
            class-specific knowledge, since the profile XML does not encode which
            subset of columns forms the natural composite key — that information
            exists only in the IEC specification prose, not the machine-readable
            profile.
            - The heuristic may include more columns than strictly necessary for the
              natural key (e.g. value columns alongside the true key columns), making
              the constraint wider than ideal but never incorrect. A constraint that
              is too wide prevents valid duplicates from being inserted; a constraint
              that is missing allows silent semantic corruption. The wider constraint
              is the safer failure mode.
            - NOTE: The parallel SQL DDL builder (sql.xsl) must apply the same
              detection and emit "id" VARCHAR(100) PRIMARY KEY plus an equivalent
              UNIQUE constraint for these classes. Both builders share the same
              cimtool:has-mrid-ancestor logic to ensure consistent output.
    -->
    <xsl:template match="a:ComplexType|a:Root">
        <item/>
        <xsl:variable name="super" select="a:SuperType[1]"/>
        <xsl:call-template name="annotate"/>
        <item>[Table(&quot;<xsl:value-of select="@name"/>&quot;)]</item>
        <!-- Emit [Index] unique constraint for each a:Compound FK column.
             a:Instance references are NOT unique — a:Compound references are
             always 1:1 by definition and must be enforced as such in EF Core.
             [Index] is a class-level attribute from Microsoft.EntityFrameworkCore
             and has no data annotation equivalent in System.ComponentModel.DataAnnotations. -->
        <xsl:for-each select="a:Compound">
            <item>[Index(nameof(<xsl:value-of select="cimtool:capitalize(@name)"/>Id), IsUnique = true)]</item>
        </xsl:for-each>

        <xsl:choose>

            <!-- ═══ Case 1: IdentifiedObject hierarchy — standard TPT path ═══ -->
            <xsl:when test="cimtool:has-mrid-ancestor(.)">
                <xsl:choose>
                    <xsl:when test="$super">
                        <item>public class <xsl:value-of select="@name"/> : <xsl:value-of select="$super/@name"/></item>
                    </xsl:when>
                    <xsl:otherwise>
                        <item>public class <xsl:value-of select="@name"/></item>
                    </xsl:otherwise>
                </xsl:choose>
                <list begin="{{" indent="    " delim="" end="}}">
                    <xsl:if test="$super">
                        <item>// Inherits from <xsl:value-of select="$super/@name"/> - configure further in EF Fluent API if needed</item>
                    </xsl:if>
                    <item>public <xsl:value-of select="@name"/>() { }</item>
                    <item>public override string ToString() { return this.GetType().Name; }</item>
                    <xsl:if test="not($super)">
                        <item/>
                        <list begin="" indent="/// " end="">
                            <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                                <item>Determines whether this instance and a specified object represent the same</item>
                                <item><xsl:value-of select="@name"/>, compared by runtime type and &lt;c&gt;MRId&lt;/c&gt;.</item>
                            </list>
                            <list begin="&lt;param name=&quot;obj&quot;&gt;" indent="" end="&lt;/param&gt;">
                                <item>The object to compare with this instance.</item>
                            </list>
                            <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                                <item>&lt;c&gt;true&lt;/c&gt; if &lt;paramref name=&quot;obj&quot;/&gt; is the same concrete type as this instance</item>
                                <item>and both have an equal, non-null &lt;c&gt;MRId&lt;/c&gt;; otherwise &lt;c&gt;false&lt;/c&gt;.</item>
                            </list>
                            <list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
                                <item>The runtime-type guard ensures two different subclasses with coincidentally</item>
                                <item>identical UUIDs are never considered equal. Returning &lt;c&gt;false&lt;/c&gt; when</item>
                                <item>&lt;c&gt;MRId&lt;/c&gt; is &lt;c&gt;null&lt;/c&gt; is consistent with the EF Core convention that a</item>
                                <item>transient (not-yet-persisted) entity is not equal to any other entity.</item>
                                <item>All subclasses inherit this implementation and must not override it.</item>
                            </list>
                        </list>
                        <item>public override bool Equals(object? obj)</item>
                        <list begin="{{" indent="    " delim="" end="}}">
                            <item>if (obj is not <xsl:value-of select="@name"/> other) return false;</item>
                            <item>if (ReferenceEquals(this, other)) return true;</item>
                            <item>if (GetType() != other.GetType()) return false;</item>
                            <item>return MRId != null &amp;&amp; MRId == other.MRId;</item>
                        </list>
                        <item/>
                        <list begin="" indent="/// " end="">
                            <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                                <item>Returns a hash code based on runtime type and &lt;c&gt;MRId&lt;/c&gt;.</item>
                            </list>
                            <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                                <item>A hash code combining the runtime type and &lt;c&gt;MRId&lt;/c&gt;, consistent with</item>
                                <item>the &lt;see cref=&quot;Equals&quot;/&gt; override. All subclasses inherit this implementation.</item>
                            </list>
                        </list>
                        <item>public override int GetHashCode() => HashCode.Combine(GetType(), MRId);</item>
                    </xsl:if>
                    <xsl:apply-templates/>
                </list>
            </xsl:when>

            <!-- ═══ Case 2: Non-IdentifiedObject — surrogate id + heuristic UNIQUE ═══
                 Sub-case 2a: Has a superclass (e.g. PhaseTapChangerTablePoint, RatioTapChangerTablePoint)
                   The entity participates in a non-IdentifiedObject TPT hierarchy. It
                   inherits the surrogate 'id' key from its parent and must NOT redeclare
                   it — doing so would create a duplicate [Key] in the EF model. The class
                   declaration emits C# inheritance exactly as Case 1 does, but without the
                   surrogate key block. The SQL schema enforces this via the inheritance FK
                   constraint (e.g. "PhaseTapChangerTablePoint"."id" REFERENCES
                   "TapChangerTablePoint"."id"), which the SQL builder emits correctly.

                 Sub-case 2b: No superclass (e.g. TapChangerTablePoint, CurveData,
                   RegularTimePoint, NonlinearShuntCompensatorPoint)
                   The entity is the root of its own hierarchy (or a standalone class) with
                   no natural single-column PK. A surrogate 'id' key is declared here. -->
            <xsl:otherwise>
                <!-- Build the heuristic UNIQUE index across all required non-surrogate
                     properties. Covers required scalar attributes and required single-valued
                     FK shadow properties. Collection associations are excluded since the FK
                     lives on the child table side, not here. Applies to both sub-cases. -->
                <xsl:variable name="required-props" as="xs:string*">
                    <xsl:for-each select="(a:Simple|a:Domain)[@minOccurs='1']">
                        <xsl:sequence select="cimtool:capitalize(@name)"/>
                    </xsl:for-each>
                    <xsl:for-each select="(a:Instance|a:Reference)[@minOccurs='1'][not(@maxOccurs) or @maxOccurs='1']">
                        <xsl:sequence select="concat(cimtool:capitalize(@name), 'Id')"/>
                    </xsl:for-each>
                </xsl:variable>
                <xsl:if test="count($required-props) gt 0">
                    <item>[Index(<xsl:value-of
                        select="string-join(for $p in $required-props return concat('nameof(', $p, ')'), ', ')"/>, IsUnique = true)]</item>
                </xsl:if>
                <xsl:choose>

                    <!-- Sub-case 2a: Has superclass — inherits surrogate key from parent -->
                    <xsl:when test="$super">
                        <item>public class <xsl:value-of select="@name"/> : <xsl:value-of select="$super/@name"/></item>
                        <list begin="{{" indent="    " delim="" end="}}">
                            <item>// Inherits surrogate 'id' key from <xsl:value-of select="$super/@name"/> — do not redeclare [Key] here.</item>
                            <item>// Neither this class nor its parent inherits from IdentifiedObject.</item>
                            <item>// EF Core maps this as TPT using the shared surrogate 'id' PK.</item>
                            <item>public <xsl:value-of select="@name"/>() { }</item>
                            <item>public override string ToString() { return this.GetType().Name; }</item>
                            <xsl:apply-templates/>
                        </list>
                    </xsl:when>

                    <!-- Sub-case 2b: No superclass — declares its own surrogate key -->
                    <xsl:otherwise>
                        <item>public class <xsl:value-of select="@name"/></item>
                        <list begin="{{" indent="    " delim="" end="}}">
                            <item>public <xsl:value-of select="@name"/>() { }</item>
                            <item>public override string ToString() { return this.GetType().Name; }</item>
                            <item/>
                            <list begin="" indent="/// " end="">
                                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                                    <item>Determines whether this instance and a specified object represent the same</item>
                                    <item>&lt;c&gt;<xsl:value-of select="@name"/>&lt;/c&gt;, compared by runtime type and surrogate &lt;c&gt;Id&lt;/c&gt;.</item>
                                </list>
                                <list begin="&lt;param name=&quot;obj&quot;&gt;" indent="" end="&lt;/param&gt;">
                                    <item>The object to compare with this instance.</item>
                                </list>
                                <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                                    <item>&lt;c&gt;true&lt;/c&gt; if &lt;paramref name=&quot;obj&quot;/&gt; is the same concrete type as this instance</item>
                                    <item>and both have an equal, non-null surrogate &lt;c&gt;Id&lt;/c&gt;; otherwise &lt;c&gt;false&lt;/c&gt;.</item>
                                </list>
                                <list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
                                    <item>This class uses a surrogate &lt;c&gt;Id&lt;/c&gt; rather than a natural &lt;c&gt;MRId&lt;/c&gt;</item>
                                    <item>because it does not inherit from IdentifiedObject. The runtime-type guard</item>
                                    <item>ensures subclasses with the same surrogate &lt;c&gt;Id&lt;/c&gt; are not considered equal.</item>
                                    <item>Returning &lt;c&gt;false&lt;/c&gt; when &lt;c&gt;Id&lt;/c&gt; is &lt;c&gt;null&lt;/c&gt; is consistent with the</item>
                                    <item>convention that a transient entity is not equal to any other entity.</item>
                                    <item>Subclasses inherit this implementation and must not override it.</item>
                                </list>
                            </list>
                            <item>public override bool Equals(object? obj)</item>
                            <list begin="{{" indent="    " delim="" end="}}">
                                <item>if (obj is not <xsl:value-of select="@name"/> other) return false;</item>
                                <item>if (ReferenceEquals(this, other)) return true;</item>
                                <item>if (GetType() != other.GetType()) return false;</item>
                                <item>return Id != null &amp;&amp; Id == other.Id;</item>
                            </list>
                            <item/>
                            <list begin="" indent="/// " end="">
                                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                                    <item>Returns a hash code based on runtime type and surrogate &lt;c&gt;Id&lt;/c&gt;.</item>
                                </list>
                                <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                                    <item>A hash code combining the runtime type and &lt;c&gt;Id&lt;/c&gt;, consistent with</item>
                                    <item>the &lt;see cref=&quot;Equals&quot;/&gt; override. Subclasses inherit this implementation.</item>
                                </list>
                            </list>
                            <item>public override int GetHashCode() => HashCode.Combine(GetType(), Id);</item>
                            <item/>
                            <list begin="" indent="/// " end="">
                                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                                    <item>Surrogate primary key — this class does not inherit from IdentifiedObject</item>
                                    <item>and has no natural single-column primary key. This surrogate 'id' is a</item>
                                    <item>persistence artefact. The [Index] above enforces a heuristic uniqueness</item>
                                    <item>constraint across all required non-surrogate columns as a guard against</item>
                                    <item>semantically corrupt duplicate rows. See the template documentation in</item>
                                    <item>the XSLT source for a full explanation of the design tradeoffs.</item>
                                </list>
                            </list>
                            <item>[Key]</item>
                            <item>[Column("id")]</item>
                            <item>[MaxLength(100)]</item>
                            <item>public string Id { get; set; } = null!;</item>
                            <xsl:apply-templates/>
                        </list>
                    </xsl:otherwise>

                </xsl:choose>
            </xsl:otherwise>

        </xsl:choose>
    </xsl:template>

    <!-- Class template for CompoundType -->
    <xsl:template match="a:CompoundType">
        <item/>
        <xsl:call-template name="annotate"/>
        <item>[Table(&quot;<xsl:value-of select="@name"/>&quot;)]</item>
        <!-- Emit [Index] unique constraint for each nested a:Compound FK column.
             CompoundTypes can themselves reference other CompoundTypes (e.g.
             StreetAddress references Status, StreetDetail, TownDetail), and those
             nested compound references are equally 1:1 and require unique indexes. -->
        <xsl:for-each select="a:Compound">
            <item>[Index(nameof(<xsl:value-of select="cimtool:capitalize(@name)"/>Id), IsUnique = true)]</item>
        </xsl:for-each>
        <item>public class <xsl:value-of select="@name"/></item>
        <list begin="{{" indent="    " delim="" end="}}">
            <list begin="" indent="/// " end="">
                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                    <item>Initialises a new &lt;c&gt;<xsl:value-of select="@name"/>&lt;/c&gt; and assigns a new UUID to &lt;c&gt;Id&lt;/c&gt;,</item>
                    <item>satisfying the compound value object ownership invariant: each compound row</item>
                    <item>has exactly one owner and must carry a unique surrogate id that is never</item>
                    <item>shared across parent columns or parent rows. See the companion</item>
                    <item>sql-rdfs-ansi92 DDL script for full details.</item>
                </list>
            </list>
            <item>public <xsl:value-of select="@name"/>() { Id = System.Guid.NewGuid().ToString(); }</item>
            <item>public override string ToString() { return this.GetType().Name; }</item>
            <item/>
            <list begin="" indent="/// " end="">
                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                    <item>Determines whether this instance and a specified object represent the same</item>
                    <item>&lt;c&gt;<xsl:value-of select="@name"/>&lt;/c&gt; compound value object, compared by surrogate &lt;c&gt;Id&lt;/c&gt;.</item>
                </list>
                <list begin="&lt;param name=&quot;obj&quot;&gt;" indent="" end="&lt;/param&gt;">
                    <item>The object to compare with this instance.</item>
                </list>
                <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                    <item>&lt;c&gt;true&lt;/c&gt; if &lt;paramref name=&quot;obj&quot;/&gt; is a &lt;c&gt;<xsl:value-of select="@name"/>&lt;/c&gt; with the same</item>
                    <item>&lt;c&gt;Id&lt;/c&gt;; otherwise &lt;c&gt;false&lt;/c&gt;.</item>
                </list>
                <list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
                    <item>Compound types are value objects — each row has exactly one owner and its</item>
                    <item>&lt;c&gt;Id&lt;/c&gt; is a UUID assigned on construction, so &lt;c&gt;Id&lt;/c&gt; is never</item>
                    <item>&lt;c&gt;null&lt;/c&gt; and no null guard is required.</item>
                </list>
            </list>
            <item>public override bool Equals(object? obj)</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>if (obj is not <xsl:value-of select="@name"/> other) return false;</item>
                <item>if (ReferenceEquals(this, other)) return true;</item>
                <item>return Id == other.Id;</item>
            </list>
            <item/>
            <list begin="" indent="/// " end="">
                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                    <item>Returns a hash code based on the surrogate &lt;c&gt;Id&lt;/c&gt; of this instance.</item>
                </list>
                <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                    <item>The hash code of &lt;c&gt;Id&lt;/c&gt;, consistent with the &lt;see cref=&quot;Equals&quot;/&gt; override.</item>
                </list>
            </list>
            <item>public override int GetHashCode() => Id.GetHashCode();</item>
            <item/>
            <item>[Key]</item>
            <item>[Column("id")]</item>
            <item>[MaxLength(100)]</item>
            <item>public string Id { get; set; } = null!;</item>
            <xsl:apply-templates/>
        </list>
    </xsl:template>

    <!-- Class template for EnumeratedType -->
    <xsl:template match="a:EnumeratedType">
        <item/>
        <xsl:call-template name="annotate"/>
        <item>[Table(&quot;<xsl:value-of select="@name"/>&quot;)]</item>
        <item>public class <xsl:value-of select="@name"/></item>
        <list begin="{{" indent="    " delim="" end="}}">
            <item>public <xsl:value-of select="@name"/>() { }</item>
            <item>public override string ToString() { return this.GetType().Name; }</item>
            <item/>
            <list begin="" indent="/// " end="">
                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                    <item>Determines whether this instance and a specified object represent the same</item>
                    <item>&lt;c&gt;<xsl:value-of select="@name"/>&lt;/c&gt; enumeration literal, compared by &lt;c&gt;Name&lt;/c&gt;.</item>
                </list>
                <list begin="&lt;param name=&quot;obj&quot;&gt;" indent="" end="&lt;/param&gt;">
                    <item>The object to compare with this instance.</item>
                </list>
                <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                    <item>&lt;c&gt;true&lt;/c&gt; if &lt;paramref name=&quot;obj&quot;/&gt; is a &lt;c&gt;<xsl:value-of select="@name"/>&lt;/c&gt; with the same</item>
                    <item>non-null &lt;c&gt;Name&lt;/c&gt;; otherwise &lt;c&gt;false&lt;/c&gt;.</item>
                </list>
                <list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
                    <item>Enumeration types are lookup tables keyed on &lt;c&gt;Name&lt;/c&gt;. Returning</item>
                    <item>&lt;c&gt;false&lt;/c&gt; when &lt;c&gt;Name&lt;/c&gt; is &lt;c&gt;null&lt;/c&gt; is consistent with the</item>
                    <item>convention that a transient entity is not equal to any other entity.</item>
                </list>
            </list>
            <item>public override bool Equals(object? obj)</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>if (obj is not <xsl:value-of select="@name"/> other) return false;</item>
                <item>if (ReferenceEquals(this, other)) return true;</item>
                <item>return Name != null &amp;&amp; Name == other.Name;</item>
            </list>
            <item/>
            <list begin="" indent="/// " end="">
                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                    <item>Returns a hash code based on the &lt;c&gt;Name&lt;/c&gt; of this enumeration literal.</item>
                </list>
                <list begin="&lt;returns&gt;" indent="" end="&lt;/returns&gt;">
                    <item>The hash code of &lt;c&gt;Name&lt;/c&gt;, or zero if &lt;c&gt;Name&lt;/c&gt; is &lt;c&gt;null&lt;/c&gt;,</item>
                    <item>consistent with the &lt;see cref=&quot;Equals&quot;/&gt; override.</item>
                </list>
            </list>
            <item>public override int GetHashCode() => Name?.GetHashCode() ?? 0;</item>
            <item/>
            <item>[Key]</item>
            <item>[Column("name")]</item>
            <item>[MaxLength(100)]</item>
            <item>public string Name { get; set; } = null!;</item>
            <item/>
            <xsl:apply-templates select="a:EnumeratedValue"/>
        </list>
    </xsl:template>

    <!-- In config mode: output each class reference using typeof(...) -->
    <xsl:template match="a:ComplexType|a:Root|a:EnumeratedType|a:CompoundType" mode="config">
        <item>typeof(<xsl:value-of select="@name"/>)</item>
    </xsl:template>

    <!-- Property template for a:Simple and a:Domain (simple attributes) -->
    <xsl:template match="a:Simple|a:Domain">
        <item/>
        <xsl:call-template name="annotate"/>
        <xsl:choose>
            <xsl:when test="@name = 'mRID'">
                <item>[Key]</item>
            </xsl:when>
            <xsl:otherwise></xsl:otherwise>
        </xsl:choose>
        <item>[Column(&quot;<xsl:value-of select="@name"/>&quot;)]</item>
        <xsl:variable name="maxLength" select="cimtool:maxLength(@xstype, @name)"/>
        <xsl:if test="fn:exists($maxLength)">
            <item><xsl:value-of select="$maxLength"/></item>
        </xsl:if>
        <xsl:choose>
            <!-- Non-nullable string PK — MRId and compound surrogate id -->
            <xsl:when test="cimtool:csType(@xstype) = 'string' and (@name = 'mRID' or @name = 'id')">
                <item>public string <xsl:value-of select="cimtool:safePropertyName(@name, @xstype, .)"/> { get; set; } = null!;</item>
            </xsl:when>
            <!-- Required string — minOccurs = 1 -->
            <xsl:when test="cimtool:csType(@xstype) = 'string' and @minOccurs = '1'">
                <item>public string <xsl:value-of select="cimtool:safePropertyName(@name, @xstype, .)"/> { get; set; } = null!;</item>
            </xsl:when>
            <!-- Optional string — minOccurs = 0 or absent -->
            <xsl:when test="cimtool:csType(@xstype) = 'string'">
                <item>public string? <xsl:value-of select="cimtool:safePropertyName(@name, @xstype, .)"/> { get; set; }</item>
            </xsl:when>
            <!-- Required value type — minOccurs = 1 (bool, int, DateTime, etc.) -->
            <xsl:when test="@minOccurs = '1'">
                <item>public <xsl:value-of select="cimtool:csType(@xstype)"/><xsl:text> </xsl:text><xsl:value-of select="cimtool:safePropertyName(@name, @xstype, .)"/> { get; set; }</item>
            </xsl:when>
            <!-- Optional value type — minOccurs = 0 or absent -->
            <xsl:otherwise>
                <item>public <xsl:value-of select="cimtool:csType(@xstype)"/>?<xsl:text> </xsl:text><xsl:value-of select="cimtool:safePropertyName(@name, @xstype, .)"/> { get; set; }</item>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Property template for a:Instance, a:Reference, and a:Compound (navigation properties).
         a:Compound covers compound-type references (e.g. electronicAddress, phone1, status,
         streetDetail) which use the same shadow FK + navigation property pattern as a:Instance.
         The distinction between compound and non-compound types is handled at the SQL DDL level;
         at the C# EF layer both produce identical property declarations.

         Shadow FK nullability:
           @minOccurs='1'  →  non-nullable string (= null!) — EF Core treats the relationship
                              as required, consistent with the NOT NULL column in the SQL DDL.
           @minOccurs='0'  →  nullable string? — EF Core treats the relationship as optional,
                              consistent with the nullable column in the SQL DDL.
         In EF Core 6+, a non-nullable FK property automatically implies a required relationship
         without needing an explicit .IsRequired() call in the Fluent API. -->
    <xsl:template match="a:Instance|a:Reference|a:Compound">
        <item/>
        <xsl:call-template name="annotate"/>
        <xsl:choose>
            <!-- Single navigation property when maxOccurs is missing or equals '1' -->
            <xsl:when test="not(@maxOccurs) or @maxOccurs = '1'">
                <!-- Shadow FK property — nullability driven by minOccurs -->
                <item>[Column(&quot;<xsl:value-of select="@name"/>&quot;)]</item>
                <item>[MaxLength(100)]</item>
                <xsl:choose>
                    <xsl:when test="@minOccurs = '1'">
                        <item>public string <xsl:value-of select="cimtool:capitalize(@name)"/>Id { get; set; } = null!;</item>
                    </xsl:when>
                    <xsl:otherwise>
                        <item>public string? <xsl:value-of select="cimtool:capitalize(@name)"/>Id { get; set; }</item>
                    </xsl:otherwise>
                </xsl:choose>
                <item/>
                <!-- Navigation property — [ForeignKey] points at the shadow property above, not the column -->
                <item>[ForeignKey(nameof(<xsl:value-of select="cimtool:capitalize(@name)"/>Id))]</item>
                <item>public virtual <xsl:value-of select="@type"/>?<xsl:text> </xsl:text><xsl:value-of select="cimtool:capitalize(@name)"/> { get; set; }</item>
            </xsl:when>
            <!-- Unbounded collection (maxOccurs > '1' or 'unbounded'): intentionally suppressed.
                 ICollection<T> navigation properties are never generated — see the assembly-level
                 <remarks> block at the top of this file for the full rationale. The FK column
                 lives on the child table; the relationship must be configured from the child side
                 via Fluent API. A self-documenting comment block is emitted in the generated
                 source in place of the suppressed property so that implementers know the
                 association exists and are shown the correct EF Core query pattern. -->
            <xsl:otherwise>
                <xsl:variable name="childType"      select="string(@type)"/>
                <xsl:variable name="assocName"      select="cimtool:capitalize(@name)"/>
                <xsl:variable name="parentType"     select="string(parent::*/@name)"/>
                <xsl:variable name="inversePropName"
                    select="if (@inverseBaseProperty)
                            then cimtool:capitalize(tokenize(string(@inverseBaseProperty), '[\.#]')[last()])
                            else ''"/>
                <xsl:variable name="parentPK"
                    select="if (cimtool:has-mrid-ancestor(parent::*)) then 'MRId' else 'Id'"/>
                <item/>
				<list begin="" indent="// " end="">
					<item>────────────────────────────────────────────────────────────────────────────────────────────────</item>
					<item> Suppressed Collection Navigation:  <xsl:value-of select="$parentType"/> → <xsl:value-of select="$childType"/>  [<xsl:value-of select="@minOccurs"/>..*] </item>
					<item>The profile declares a [<xsl:value-of select="@minOccurs"/>..*] association on this class to <xsl:value-of select="$childType"/>.</item>
					<item>A collection navigation property has been intentionally suppressed:</item>
					<item/>
					<list begin="" indent="  " end="">
						<item>// NOT generated</item>
						<item>public virtual ICollection&lt;<xsl:value-of select="$childType"/>&gt; <xsl:value-of select="$assocName"/> { get; set; }</item>
					</list>
					<item/>
					<item>See the assembly-level &lt;remarks&gt; block at the top of this file for the full rationale.</item>
					<item>In summary: unbounded collections risk loading entire result sets into memory with no</item>
					<item>pagination, are not consistently present across profiles, and introduce serialisation</item>
					<item>and change-tracking fragility. Instead, use an explicit LINQ query such as:</item>
					<item/>
					<item>Basic traversal:</item>
					<list begin="" indent="  " end="">
						<item>IQueryable&lt;<xsl:value-of select="$childType"/>&gt; results = ctx.Set&lt;<xsl:value-of select="$childType"/>&gt;()</item>
						<list begin="" indent="  " end="">
							<item>.Where(x => x.<xsl:value-of select="$inversePropName"/>Id == this.<xsl:value-of select="$parentPK"/>);</item>
						</list>
					</list>
					<item/>
					<item>Paginated traversal:</item>
					<list begin="" indent="  " end="">
						<item>var page = await ctx.Set&lt;<xsl:value-of select="$childType"/>&gt;()</item>
						<list begin="" indent="  " end="">
							<item>.Where(x => x.<xsl:value-of select="$inversePropName"/>Id == this.<xsl:value-of select="$parentPK"/>)</item>
							<item>.OrderBy(x => x.MRId)</item>
							<item>.OrderBy(x => x.MRId)</item>
							<item>.Skip(offset).Take(pageSize)</item>
							<item>.ToListAsync();</item>
						</list>
					</list>
					<item>────────────────────────────────────────────────────────────────────────────────────────────────</item>
				</list>
				<item/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Property template for a:Enumerated (enumerated property as a FK string reference) -->
    <xsl:template match="a:Enumerated">
        <item/>
        <xsl:call-template name="annotate"/>
        <item>[Column(&quot;<xsl:value-of select="@name"/>&quot;)]</item>
        <item>[MaxLength(100)]</item>
        <xsl:choose>
            <!-- Required enumerated reference — minOccurs = 1 -->
            <xsl:when test="@minOccurs = '1'">
                <item>public string <xsl:value-of select="cimtool:safePropertyName(@name, '', .)"/> { get; set; } = null!;</item>
            </xsl:when>
            <!-- Optional enumerated reference — minOccurs = 0 or absent -->
            <xsl:otherwise>
                <item>public string? <xsl:value-of select="cimtool:safePropertyName(@name, '', .)"/> { get; set; }</item>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
	<!-- 
		Template for enumerated values inside an EnumeratedType.
		cimtool:safeIdentifier() is used rather than cimtool:capitalize() to preserve
		case, preventing duplicate const identifiers for enum values that differ
		only in case (e.g. UnitSymbol 'H' henry vs 'h' hour). 
	-->
	<xsl:template match="a:EnumeratedValue">
    	<!-- 
    		We very intentionally do NOT call capitalize as some enumerations (e.g. UnitSymbol) can have h and H as two
    		distinct literals. This introduces compilation issues in C# so we honor the case sensitivity in that scenario.  
    	-->
	    <item>public const string <xsl:value-of select="cimtool:safeIdentifier(@name)"/> = &quot;<xsl:value-of select="@name"/>&quot;;</item>
	</xsl:template>

    <!--
        Configuration block listing all classes in topological order.
        Receives pre-computed sorted sequences from the a:Catalog template
        to avoid rebuilding the dependency maps a second time.
    -->
    <xsl:template name="config">
        <xsl:param name="sorted-compounds" as="element()*"/>
        <xsl:param name="sorted-classes"   as="element()*"/>
        <item/>
        <item>public static readonly System.Type[] allClasses = new System.Type[]</item>
        <list begin="{{" indent="    " delim="," end="}};">
            <!-- Tier 1: EnumeratedType — lookup tables, no dependencies -->
            <xsl:apply-templates select="a:EnumeratedType" mode="config"/>
            <!-- Tier 2: CompoundType — in topological dependency order -->
            <xsl:apply-templates select="$sorted-compounds" mode="config"/>
            <!-- Tier 3: ComplexType + Root — superclasses before subclasses -->
            <xsl:apply-templates select="$sorted-classes" mode="config"/>
        </list>
        <item/>
    </xsl:template>

    <!--
        Generates two artifacts inside the outer $envelope class:

        1. ModelConfiguration — nested static class with the complete EF Core
           Fluent API configuration for all entities in the profile.
           Structure:
             - One public static ConfigureModel() entry point calling a private
               static method per entity in topological order.
             - One private static Configure<EntityName>() method per entity with:
                 ToTable() for explicit TPT table mapping.
                 HasOne().WithMany().HasForeignKey().OnDelete(...) for every
                 a:Compound, a:Instance, and a:Reference child relationship.
             - Entities with no FKs use a concise arrow expression body.
             - Entities with FKs use a block body with a local variable.

        2. DbContextBase — abstract DbContext subclass with generated DbSet<T>
           properties (Tier 3 entity classes only), OnModelCreating wiring,
           SaveChanges / SaveChangesAsync overrides for compound orphan cleanup,
           and the CollectOrphan / DeleteCompound infrastructure helpers.

        RESTRICT VS CLIENTNOACTION — DESIGN RATIONALE
        ──────────────────────────────────────────────
        Two distinct DeleteBehavior values are emitted depending on relationship kind:

          a:Compound:  DeleteBehavior.Restrict
            Compound types are CIM value objects — each compound row has exactly one
            owner and must never be deleted directly. Restrict enforces this invariant
            at the EF Core change-tracker layer: if application code attempts to
            delete a tracked compound while its parent is also tracked, EF Core
            throws before the operation reaches the DB. Compound cleanup is handled
            in the correct direction by the generated SaveChanges overrides in
            DbContextBase: the parent entity is deleted first in the first save,
            then orphaned compound rows are found and deleted in a second save.
            The companion SQL DDL emits ON DELETE CASCADE for these FKs as a
            safety net at the database level.

          a:Instance | a:Reference:  DeleteBehavior.ClientNoAction
            Associations between independent IdentifiedObject entities carry no
            DB-level cascade. Deleting one end must not silently delete the other.
            ClientNoAction leaves cascade responsibility entirely to the application,
            which is correct and safe for these semantically independent associations.
    -->
    <xsl:template name="dbcontext">
        <xsl:param name="sorted-compounds" as="element()*"/>
        <xsl:param name="sorted-classes"   as="element()*"/>
        <item/>
        <list begin="" indent="/// " end="">
            <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                <item>Generated EF Core Fluent API configuration for the <xsl:value-of select="$envelope"/> profile.</item>
                <item>Called automatically from &lt;c&gt;DbContextBase.OnModelCreating&lt;/c&gt; — no</item>
                <item>manual wiring is required when using the generated &lt;c&gt;DbContextBase&lt;/c&gt;.</item>
                <item>This class is fully regenerated by CIMTool — do not edit manually.</item>
            </list>
            <list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
                <list begin="&lt;para&gt;" indent="" end="&lt;/para&gt;">
                    <item>&lt;b&gt;Restrict vs ClientNoAction Delete Behavior&lt;/b&gt;&lt;br/&gt;</item>
                    <item>Two distinct &lt;c&gt;DeleteBehavior&lt;/c&gt; values are configured depending on</item>
                    <item>relationship kind:</item>
                    <list begin="&lt;list type=&quot;bullet&quot;&gt;" indent="   " end=" &lt;/list&gt;">
                        <list begin="&lt;item&gt;" indent="  " end="&lt;/item&gt;">
                            <item>&lt;c&gt;DeleteBehavior.Restrict&lt;/c&gt; — CIM Compound type references</item>
                            <item>(e.g. &lt;c&gt;ElectronicAddress&lt;/c&gt;, &lt;c&gt;Status&lt;/c&gt;, &lt;c&gt;TelephoneNumber&lt;/c&gt;).</item>
                            <item>Compounds are value objects with exactly one owner. Direct compound</item>
                            <item>deletion violates the ownership invariant and is rejected by EF Core</item>
                            <item>at the change-tracker layer. Compound cleanup is handled in the correct</item>
                            <item>direction — parent deleted first, then orphaned compounds — by the</item>
                            <item>generated &lt;c&gt;SaveChanges&lt;/c&gt; overrides in &lt;c&gt;DbContextBase&lt;/c&gt;.</item>
                            <item>The companion SQL DDL emits &lt;c&gt;ON DELETE CASCADE&lt;/c&gt; as a safety net</item>
                            <item>at the database level.</item>
                        </list>
                        <list begin="&lt;item&gt;" indent="  " end="&lt;/item&gt;">
                            <item>&lt;c&gt;DeleteBehavior.ClientNoAction&lt;/c&gt; — associations between independent</item>
                            <item>IdentifiedObject entities. These carry no DB-level cascade and deleting</item>
                            <item>one end must not silently delete the other. Cascade responsibility is</item>
                            <item>left entirely to the application.</item>
                        </list>
                    </list>
                </list>
            </list>
        </list>
        <item>public static class ModelConfiguration</item>
        <list begin="{{" indent="    " delim="" end="}}">
            <!-- Entry point — calls one private method per entity in topological order -->
            <item>public static void ConfigureModel(ModelBuilder modelBuilder)</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <!-- Tier 1: EnumeratedType -->
                <xsl:for-each select="a:EnumeratedType">
                    <item>Configure<xsl:value-of select="@name"/>(modelBuilder);</item>
                </xsl:for-each>
                <!-- Tier 2: CompoundType in topological dependency order -->
                <xsl:for-each select="$sorted-compounds">
                    <item>Configure<xsl:value-of select="@name"/>(modelBuilder);</item>
                </xsl:for-each>
                <!-- Tier 3: ComplexType + Root — superclasses before subclasses -->
                <xsl:for-each select="$sorted-classes">
                    <item>Configure<xsl:value-of select="@name"/>(modelBuilder);</item>
                </xsl:for-each>
            </list>
            <!-- Private configuration methods — one per entity -->
            <xsl:apply-templates select="a:EnumeratedType" mode="dbcontext"/>
            <xsl:apply-templates select="$sorted-compounds" mode="dbcontext"/>
            <xsl:apply-templates select="$sorted-classes" mode="dbcontext"/>
        </list>
        <item/>
        <!--
            DbContextBase — generated abstract base DbContext.

            Emitted inside the outer $envelope class so that consumers reference it as
            $envelope.DbContextBase (e.g. SampleProfile.DbContextBase), keeping the
            profile namespace as the natural qualifier without redundant naming.

            DbSet<T> properties are generated for Tier 3 entity classes only
            (a:Root and a:ComplexType). Compound types and enumerated types are
            intentionally excluded — see the file-level <remarks> for rationale.

            SaveChanges / SaveChangesAsync overrides handle compound orphan cleanup
            in the correct direction (parent deleted first, then orphaned compounds),
            relying on DeleteBehavior.Restrict in ModelConfiguration to enforce the
            ownership invariant at the EF Core layer.
        -->
        <list begin="" indent="/// " end="">
            <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                <item>Generated abstract base DbContext for the <xsl:value-of select="$envelope"/> profile.</item>
                <item>Subclass this once in your project to obtain a fully functional EF Core</item>
                <item>DbContext with compound orphan cleanup and all entity DbSet properties.</item>
            </list>
            <list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
                <item>This class is fully regenerated by CIMTool — do not edit manually.</item>
                <item>Compound lifecycle is managed automatically: &lt;c&gt;SaveChanges&lt;/c&gt; and</item>
                <item>&lt;c&gt;SaveChangesAsync&lt;/c&gt; detect and delete orphaned compound rows after</item>
                <item>each save. No hand-written cleanup logic is required. Create the</item>
                <item>following thin subclass once in your project:</item>
                <list begin="&lt;code&gt;" indent="" end="&lt;/code&gt;">
                    <item>public class <xsl:value-of select="$envelope"/>DbContext : <xsl:value-of select="$envelope"/>.DbContextBase</item>
                    <list begin="{{" indent="    " end="}}">
                        <item>public <xsl:value-of select="$envelope"/>DbContext(DbContextOptions&lt;<xsl:value-of select="$envelope"/>DbContext&gt; options)</item>
                        <list begin="" indent="    " end="">
                        	<item>: base(options) { }</item>
                        </list>
                    </list>
                </list>
            </list>
        </list>
        <item>public abstract class DbContextBase : DbContext</item>
        <list begin="{{" indent="    " delim="" end="}}">
            <list begin="" indent="// " end="">
                <item>DbSet&lt;T&gt; properties are generated for persistent CIM entity classes only —</item>
                <item>classes with their own primary key (mRID or surrogate id). Compound types</item>
                <item>and enumerated types are intentionally excluded: compounds are value objects</item>
                <item>owned exclusively by their parent —</item>
                <item>exposing them as DbSet&lt;T&gt; would invite independent querying and direct</item>
                <item>deletion, violating the ownership invariant. Enumerated types are referenced</item>
                <item>only by string literals and never queried at runtime in normal application</item>
                <item>code. Both are accessible via context.Set&lt;T&gt;() when direct access is needed.</item>
                <item>See the file-level &lt;remarks&gt; for the full rationale.</item>
            </list>
            <!-- One DbSet<T> property per Tier 3 entity class in inheritance-safe order -->
            <xsl:for-each select="$sorted-classes">
                <item>public DbSet&lt;<xsl:value-of select="$envelope"/>.<xsl:value-of select="@name"/>&gt; <xsl:value-of select="@name"/>s =&gt; Set&lt;<xsl:value-of select="$envelope"/>.<xsl:value-of select="@name"/>&gt;();</item>
            </xsl:for-each>
            <item/>
            <list begin="" indent="/// " end="">
                <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                    <item>Initialises a new instance of &lt;c&gt;DbContextBase&lt;/c&gt; with the supplied</item>
                    <item>EF Core options. Use this constructor when registering the context</item>
                    <item>with a dependency injection container:</item>
                    <list begin="&lt;code&gt;" indent="" end="&lt;/code&gt;">
                        <item>services.AddDbContext&lt;<xsl:value-of select="$envelope"/>DbContext&gt;(options =&gt;</item>
                        <list begin="" indent="    " end="">
                            <item>options.UseSqlServer(connectionString));</item>
                        </list>
                    </list>
                </list>
                <list begin="&lt;param name=&quot;options&quot;&gt;" indent="" end="&lt;/param&gt;">
                    <item>The EF Core options for this context, typically supplied by the</item>
                    <item>host's dependency injection container.</item>
                </list>
            </list>
            <item>protected DbContextBase(DbContextOptions options) : base(options) { }</item>
            <item/>
            <item>protected override void OnModelCreating(ModelBuilder modelBuilder)</item>
            <list begin="" indent="    " end="">
            	<item>=&gt; <xsl:value-of select="$envelope"/>.ModelConfiguration.ConfigureModel(modelBuilder);</item>
            </list>
            <item/>
            <item>public override int SaveChanges()</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>var orphans = CollectCompoundOrphans();</item>
                <item>var rows = base.SaveChanges();</item>
                <item>if (orphans.Count == 0) return rows;</item>
                <item>DeleteOrphanedCompounds(orphans);</item>
                <item>return rows + base.SaveChanges();</item>
            </list>
            <item/>
            <item>public override int SaveChanges(bool acceptAllChangesOnSuccess)</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>var orphans = CollectCompoundOrphans();</item>
                <item>var rows = base.SaveChanges(acceptAllChangesOnSuccess);</item>
                <item>if (orphans.Count == 0) return rows;</item>
                <item>DeleteOrphanedCompounds(orphans);</item>
                <item>return rows + base.SaveChanges(acceptAllChangesOnSuccess);</item>
            </list>
            <item/>
            <item>public override async Task&lt;int&gt; SaveChangesAsync(CancellationToken cancellationToken = default)</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>var orphans = CollectCompoundOrphans();</item>
                <item>var rows = await base.SaveChangesAsync(cancellationToken);</item>
                <item>if (orphans.Count == 0) return rows;</item>
                <item>DeleteOrphanedCompounds(orphans);</item>
                <item>return rows + await base.SaveChangesAsync(cancellationToken);</item>
            </list>
            <item/>
            <item>public override async Task&lt;int&gt; SaveChangesAsync(bool acceptAllChangesOnSuccess, CancellationToken cancellationToken = default)</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>var orphans = CollectCompoundOrphans();</item>
                <item>var rows = await base.SaveChangesAsync(acceptAllChangesOnSuccess, cancellationToken);</item>
                <item>if (orphans.Count == 0) return rows;</item>
                <item>DeleteOrphanedCompounds(orphans);</item>
                <item>return rows + await base.SaveChangesAsync(acceptAllChangesOnSuccess, cancellationToken);</item>
            </list>
            <item/>
            <!--
                CollectCompoundOrphans — two-phase generation.

                Phase 1: generated per entity that has a:Compound children (both Tier 3
                entities and compound types that themselves reference nested compounds).
                Iterates ChangeTracker entries with state Modified or Deleted, snapshotting
                original FK values before the first save so that replaced or nulled compound
                IDs are captured correctly.

                Phase 2: generated only for compound types that themselves have a:Compound
                children. Handles the case where a compound is orphaned via its parent's FK
                change and therefore remains Unchanged in the change tracker — invisible to
                Phase 1. A do/while loop expands the orphan list iteratively until no new
                entries are added, correctly handling compound hierarchies of arbitrary depth
                (two levels, three levels, etc.). A deduplication guard on each orphans.Add
                call prevents re-processing already-expanded entries and ensures the loop
                terminates cleanly.
            -->
            <item>private List&lt;(Type Type, string Id)&gt; CollectCompoundOrphans()</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>ChangeTracker.DetectChanges();</item>
                <item>var orphans = new List&lt;(Type Type, string Id)&gt;();</item>
                <xsl:for-each select="($sorted-compounds, $sorted-classes)[a:Compound]">
                    <xsl:variable name="entityName" select="string(@name)"/>
                    <item/>
                    <item>foreach (var entry in ChangeTracker.Entries&lt;<xsl:value-of select="$envelope"/>.<xsl:value-of select="$entityName"/>&gt;())</item>
                    <list begin="{{" indent="    " delim="" end="}}">
                        <item>if (entry.State is not EntityState.Modified and not EntityState.Deleted) continue;</item>
                        <xsl:for-each select="a:Compound">
                            <item>CollectOrphan(entry, nameof(<xsl:value-of select="$envelope"/>.<xsl:value-of select="$entityName"/>.<xsl:value-of select="cimtool:capitalize(@name)"/>Id), typeof(<xsl:value-of select="$envelope"/>.<xsl:value-of select="@type"/>), orphans);</item>
                        </xsl:for-each>
                    </list>
                </xsl:for-each>
                <xsl:if test="$sorted-compounds[a:Compound]">
                    <item/>
                    <list begin="" indent="// " end="">
                        <item>Phase 2: expand orphans for compound types that have their own compound children.</item>
                        <item>Compounds orphaned via a parent FK change remain Unchanged in the change tracker</item>
                        <item>and are invisible to Phase 1. The do/while loop handles arbitrary nesting depth —</item>
                        <item>each iteration discovers the next level of nested compounds until no new entries</item>
                        <item>are added. The deduplication guard prevents re-processing already-expanded entries.</item>
                    </list>
                    <item>int countBefore;</item>
                    <item>do</item>
                    <list begin="{{" indent="    " delim="" end="}}">
                        <item>countBefore = orphans.Count;</item>
                        <xsl:for-each select="$sorted-compounds[a:Compound]">
                            <xsl:variable name="compoundName" select="string(@name)"/>
                            <item/>
                            <item>foreach (var (_, id) in orphans.Where(o =&gt; o.Type == typeof(<xsl:value-of select="$envelope"/>.<xsl:value-of select="$compoundName"/>)).ToList())</item>
                            <list begin="{{" indent="    " delim="" end="}}">
                                <item>var entity = Set&lt;<xsl:value-of select="$envelope"/>.<xsl:value-of select="$compoundName"/>&gt;().Local.FirstOrDefault(x =&gt; x.Id == id)</item>
                                <list begin="" indent="    " end="">
                                    <item>?? Set&lt;<xsl:value-of select="$envelope"/>.<xsl:value-of select="$compoundName"/>&gt;().FirstOrDefault(x =&gt; x.Id == id);</item>
                                </list>
                                <item>if (entity is null) continue;</item>
                                <xsl:for-each select="a:Compound">
                                    <item>if (!string.IsNullOrWhiteSpace(entity.<xsl:value-of select="cimtool:capitalize(@name)"/>Id)</item>
                                    <list begin="" indent="    " end="">
                                        <item>&amp;&amp; !orphans.Any(o =&gt; o.Type == typeof(<xsl:value-of select="$envelope"/>.<xsl:value-of select="@type"/>) &amp;&amp; o.Id == entity.<xsl:value-of select="cimtool:capitalize(@name)"/>Id))</item>
                                    </list>
                                    <list begin="" indent="    " end="">
                                        <item>orphans.Add((typeof(<xsl:value-of select="$envelope"/>.<xsl:value-of select="@type"/>), entity.<xsl:value-of select="cimtool:capitalize(@name)"/>Id!));</item>
                                    </list>
                                </xsl:for-each>
                            </list>
                        </xsl:for-each>
                    </list>
                    <item>while (orphans.Count != countBefore);</item>
                </xsl:if>
                <item/>
                <item>return orphans;</item>
            </list>
            <item/>
            <!--
                DeleteOrphanedCompounds — generated per compound type in topological order.
                Topological order ensures nested compounds (e.g. Status, StreetDetail) are
                processed before compound parents (e.g. StreetAddress). The SQL DDL's
                ON DELETE CASCADE handles any additional nested cleanup at the DB level.
            -->
            <item>private void DeleteOrphanedCompounds(List&lt;(Type Type, string Id)&gt; orphans)</item>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>foreach (var (type, id) in orphans)</item>
                <list begin="{{" indent="    " delim="" end="}}">
                    <xsl:for-each select="$sorted-compounds">
                        <xsl:choose>
                            <xsl:when test="position() = 1">
                                <item>if (type == typeof(<xsl:value-of select="$envelope"/>.<xsl:value-of select="@name"/>))</item>
                            </xsl:when>
                            <xsl:otherwise>
                                <item>else if (type == typeof(<xsl:value-of select="$envelope"/>.<xsl:value-of select="@name"/>))</item>
                            </xsl:otherwise>
                        </xsl:choose>
                        <list begin="" indent="    " end="">
                            <item>DeleteCompound(Set&lt;<xsl:value-of select="$envelope"/>.<xsl:value-of select="@name"/>&gt;(), x =&gt; x.Id == id);</item>
                        </list>
                    </xsl:for-each>
                </list>
            </list>
            <item/>
            <!-- Profile-independent static helper — not generated from profile XML -->
            <item>private static void CollectOrphan&lt;TEntity&gt;(</item>
            <list begin="" indent="    " end="">
            	<item>EntityEntry&lt;TEntity&gt; entry, string propertyName,</item>
            	<item>Type compoundType, List&lt;(Type, string)&gt; orphans)</item>
            	<item>where TEntity : class</item>
            </list>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>var originalId = entry.OriginalValues[propertyName] as string;</item>
                <item>var currentId  = entry.State == EntityState.Deleted ? null</item>
                <list begin="" indent="    " end="">
                	<item>: entry.CurrentValues[propertyName] as string;</item>
                </list>
                <item>if (!string.IsNullOrWhiteSpace(originalId) &amp;&amp; originalId != currentId)</item>
                <list begin="" indent="    " delim="" end="">
                	<item>orphans.Add((compoundType, originalId));</item>
                </list>
            </list>
            <item/>
            <!--
                DeleteCompound — checks the Local in-memory cache first (compiled predicate,
                O(n) scan of tracked entities) then falls back to a database query using the
                Expression<Func> predicate, which EF Core translates to a parameterised SQL
                WHERE clause. This avoids a full-table scan while loading the entity into the
                change tracker so that Remove() can be tracked correctly.
            -->
            <item>private void DeleteCompound&lt;TEntity&gt;(</item>
            <list begin="" indent="    " end="">
                <item>DbSet&lt;TEntity&gt; set, Expression&lt;Func&lt;TEntity, bool&gt;&gt; predicate)</item>
                <item>where TEntity : class</item>
            </list>
            <list begin="{{" indent="    " delim="" end="}}">
                <item>var entity = set.Local.FirstOrDefault(predicate.Compile())</item>
				<list begin="" indent="    " end="">
					<item>?? set.FirstOrDefault(predicate);</item>
				</list>
                <item>if (entity is not null)</item>
                <list begin="" indent="    " end="">
                	<item>Remove(entity);</item>
                </list>
            </list>
        </list>
    </xsl:template>

    <!--
        Generates a private static configuration method for a single entity.
        Entities with no FK relationships use a concise arrow expression body.
        Entities with a:Compound, a:Instance, or a:Reference children use a
        block body with a local variable to chain the relationship configuration.

        DeleteBehavior is differentiated by relationship kind:
          a:Compound   →  DeleteBehavior.Restrict
            Compound type rows are value objects owned exclusively by their parent.
            Direct compound deletion violates the ownership invariant and is rejected
            by EF Core at the change-tracker layer. Compound cleanup is handled in
            the correct direction by the generated SaveChanges overrides in
            DbContextBase: parent is deleted first, then orphaned compounds are
            removed in a second save. The companion SQL DDL emits ON DELETE CASCADE
            as a safety net at the database level.
          a:Instance | a:Reference  →  DeleteBehavior.ClientNoAction
            Independent IdentifiedObject associations carry no DB-level cascade.
            Deleting one end must not silently remove the other; application code
            is responsible for managing the lifecycle of associated entities.
    -->
    <xsl:template match="a:EnumeratedType|a:CompoundType|a:ComplexType|a:Root" mode="dbcontext">
        <item/>
        <xsl:variable name="fks" select="a:Compound|a:Instance|a:Reference"/>
        <xsl:choose>
            <!-- No FK relationships — concise arrow expression body.
                 typeof() is used instead of the generic Entity<T>() to avoid a known
                 CIMTool Indent renderer bug: when an entity name coincides with a valid
                 XML element name (e.g. "Name", "Type", "Value"), the renderer
                 misinterprets <EntityName> in item text content as a child XML element
                 rather than literal text, producing garbled output (e.g. Entity<n>).
                 typeof(X) uses parentheses rather than angle brackets, sidestepping
                 the issue entirely. ModelBuilder.Entity(Type) returns EntityTypeBuilder
                 (non-generic), and ToTable() is available on that base class, so this
                 is fully equivalent for entities that require only table mapping.
                 NOTE: the FK block body case below still uses Entity<T>() because the
                 generic form is required for lambda-based HasOne/HasForeignKey calls.
                 That path is only affected if an entity with FK relationships also has
                 a name that is a valid XML element name — unlikely in practice, but
                 worth noting as a known Indent renderer limitation. The proper fix is
                 in CIMTool's Java Indent post-processor. -->
            <xsl:when test="not($fks)">
                <item>private static void Configure<xsl:value-of select="@name"/>(ModelBuilder modelBuilder)</item>
                <list begin="" indent="    " end="">
					<item>=> modelBuilder.Entity(typeof(<xsl:value-of select="@name"/>)).ToTable(&quot;<xsl:value-of select="@name"/>&quot;);</item>
				</list>
            </xsl:when>
            <!-- Has FK relationships — block body with local variable -->
            <xsl:otherwise>
                <item>private static void Configure<xsl:value-of select="@name"/>(ModelBuilder modelBuilder)</item>
                <list begin="{{" indent="    " delim="" end="}}">
                    <item>var e = modelBuilder.Entity&lt;<xsl:value-of select="@name"/>&gt;();</item>
                    <item>e.ToTable(&quot;<xsl:value-of select="@name"/>&quot;);</item>
                    <xsl:for-each select="$fks">
                        <xsl:choose>
                            <!-- Compound: value object — Restrict enforces the ownership invariant.
                                 Direct compound deletion is rejected at the EF Core layer.
                                 Compound cleanup is handled by the generated SaveChanges
                                 overrides in DbContextBase. See the ModelConfiguration
                                 class <remarks> for the full design rationale. -->
                            <xsl:when test="self::a:Compound">
                                <item>e.HasOne(x => x.<xsl:value-of select="cimtool:capitalize(@name)"/>).WithMany().HasForeignKey(x => x.<xsl:value-of select="cimtool:capitalize(@name)"/>Id).OnDelete(DeleteBehavior.Restrict);</item>
                            </xsl:when>
                            <!-- Unbounded Reference [0..*]: FK lives on the child table.
                                 No HasOne/HasMany is emitted on this (parent) side — doing so
                                 would reference a shadow FK property that does not exist on this
                                 entity. The relationship must be configured from the child side.
                                 A comment is emitted here to make that expectation explicit and
                                 to provide a copy-pasteable Fluent API starting point. -->
                            <xsl:when test="self::a:Reference and (@maxOccurs='unbounded' or number(@maxOccurs) > 1)">
                                <xsl:variable name="childType" select="string(@type)"/>
                                <xsl:variable name="parentType" select="string(parent::*/@name)"/>
                                <xsl:variable name="inversePropName"
                                    select="if (@inverseBaseProperty)
                                            then cimtool:capitalize(tokenize(string(@inverseBaseProperty), '[\.#]')[last()])
                                            else ''"/>
                                <item/>
								<list begin="" indent="// " end="">
									<item>────────────────────────────────────────────────────────────────────────────────────────────────</item>
									<item>Suppressed Fluent API:  <xsl:value-of select="$parentType"/> → <xsl:value-of select="$childType"/>  [<xsl:value-of select="@minOccurs"/>..*]</item>
									<item>The FK column for this [<xsl:value-of select="@minOccurs"/>..*] association lives on <xsl:value-of select="$childType"/>, not here.</item>
									<item>HasOne/HasMany is intentionally not configured on this parent side because</item>
									<item>no shadow FK property exists on <xsl:value-of select="$parentType"/> (see the suppressed collection</item>
									<item>navigation comment in the entity class above). Instead, configure from the child side:</item>
									<item/>
									<list begin="" indent="  " end="">
										<item>modelBuilder.Entity&lt;<xsl:value-of select="$childType"/>&gt;()</item>
										<list begin="" indent="  " end="">
											<item>.HasOne(x => x.<xsl:value-of select="$inversePropName"/>)</item>
											<item>.WithMany()</item>
											<item>.HasForeignKey(x => x.<xsl:value-of select="$inversePropName"/>Id)</item>
											<item>.OnDelete(DeleteBehavior.ClientNoAction);</item>
										</list>
									</list>
									<item>────────────────────────────────────────────────────────────────────────────────────────────────</item>
								</list>
                            </xsl:when>
                            <!-- Instance/Reference: independent entity — no cascade -->
                            <xsl:otherwise>
                                <item>e.HasOne(x => x.<xsl:value-of select="cimtool:capitalize(@name)"/>).WithMany().HasForeignKey(x => x.<xsl:value-of select="cimtool:capitalize(@name)"/>Id).OnDelete(DeleteBehavior.ClientNoAction);</item>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:for-each>
                </list>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        Generates a C# XML doc comment block from any a:Comment or a:Note
        children on the current element. Suppressed entirely when no such
        children exist, avoiding empty doc comment blocks on elements like
        ParentOrganization that carry no description in the profile.

        Pattern mirrors the file-level header:
          - Outer <list begin="" indent="/// " end=""> applies the /// prefix
            to every line without emitting spurious blank /// lines at the
            begin and end positions.
          - a:Comment content is wrapped in <summary>...</summary>.
          - a:Note content, when present, is wrapped in <remarks>...</remarks>.
          - Each XML doc tag is its own inner list so the open and close tags
            each land on their own /// -prefixed line.
          - Text content uses <wrap width="70"> to honour the column limit.
    -->
    <xsl:template name="annotate">
        <xsl:if test="a:Comment or a:Note">
            <list begin="" indent="/// " end="">
                <xsl:if test="a:Comment">
                    <list begin="&lt;summary&gt;" indent="" end="&lt;/summary&gt;">
                        <xsl:apply-templates select="a:Comment" mode="annotate"/>
                    </list>
                </xsl:if>
                <xsl:if test="a:Note">
                    <list begin="&lt;remarks&gt;" indent="" end="&lt;/remarks&gt;">
                        <xsl:apply-templates select="a:Note" mode="annotate"/>
                    </list>
                </xsl:if>
            </list>
        </xsl:if>
    </xsl:template>

    <xsl:template match="a:Comment|a:Note" mode="annotate">
        <wrap width="70">
            <xsl:value-of select="."/>
        </wrap>
    </xsl:template>

    <!--
        Suppresses inverse navigation properties entirely.

        InverseInstance and InverseReference represent the one side of a one-to-many
        association (e.g. "all Equipment belonging to this EquipmentContainer"). These
        are omitted by design, not by accident. The rationale is documented in full in
        the generated file header <remarks> block, but summarized here for maintainers:

          1. CIM associations are unbounded. Materializing an ICollection<T> with no
             pagination is a runtime hazard for large profiles such as CGMES CoreEquipment.
          2. Bidirectional mappings require both sides to be kept in sync on every
             add/remove, adding fragility and bug surface to consumer code.
          3. They introduce circular-reference risk during JSON serialization.
          4. Inverse associations are profile-dependent — not every profile includes them,
             so generated ICollection<T> properties would be inconsistent across profiles.
          5. Explicit LINQ queries (query through the child side via the shadow FK property)
             support pagination, filtering, and sorting that a collection property cannot.

        The correct pattern for consumers is to query through the child side:

            var units = context.Set<PowerElectronicsUnit>()
                .Where(u => u.PowerElectronicsConnectionId == connection.MRId)
                .ToList();

        This template must be explicit (rather than relying on the default text() suppressor)
        so that the intent is self-documenting for future maintainers of this builder.
    -->
    <xsl:template match="a:InverseInstance|a:InverseReference"/>

    <!-- Suppress text nodes -->
    <xsl:template match="text()"/>
    <xsl:template match="node()" mode="config"/>
    <xsl:template match="node()" mode="annotate"/>
    <xsl:template match="node()" mode="dbcontext"/>

</xsl:stylesheet>
