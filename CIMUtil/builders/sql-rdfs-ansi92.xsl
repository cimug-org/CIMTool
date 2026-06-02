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
	xmlns:cimtool="http://cimtool.ucaiug.io/functions"
	xmlns="http://langdale.com.au/2009/Indent">

	<xsl:output indent="yes" method="xml" encoding="UTF-8" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="envelope">Profile</xsl:param>

	<xsl:template name="ident">
		<xsl:param name="name" select="@name"/>
		<xsl:text>"</xsl:text><xsl:value-of select="translate($name, ' ', '_')"/><xsl:text>"</xsl:text>
	</xsl:template>
	
	<!-- NOTE: CHAR VARYING (or CHARACTER VARYING) is the ANSI SQL standard term. The more 
		 commonly known equivalent type is VARCHAR which is what has been used here. Here
		 the type corresponds to an assumed UUID. This relevant for mRID and ID PKs -->
	<xsl:param name="uuidType">VARCHAR(100)</xsl:param>

	<!--
		BEGIN: HIERARCHY DETECTION FUNCTION

	    Returns true if the given CIM profile element, or any ancestor reached by
	    walking the a:SuperType chain, directly declares an attribute named 'mRID'.
	    This is the definitive programmatic test for membership in the
	    IdentifiedObject hierarchy within the profile catalog XML.

	    BACKGROUND
	    ──────────
	    In the CIM, IdentifiedObject is the universal root class for all persistent,
	    independently-addressable objects. It carries the 'mRID' (master resource
	    identifier) attribute. The vast majority of CIM classes inherit from
	    IdentifiedObject and receive 'mRID' as their natural primary key.

	    A small but important subset of CIM classes do NOT inherit from
	    IdentifiedObject. These are typically collection-member or point-data
	    classes, for example:
	      - CurveData                      (owned by Curve)
	      - RegularTimePoint               (owned by RegularIntervalSchedule)
	      - NonlinearShuntCompensatorPoint (owned by NonlinearShuntCompensator)
	      - TapChangerTablePoint           (base of PhaseTapChangerTablePoint,
	                                        RatioTapChangerTablePoint)

	    These classes have no natural single-column primary key derivable from 
	    the CIM model. The IEC specification identifies natural composite keys
	    (e.g. IntervalSchedule + sequenceNumber for RegularTimePoint), but this
	    information is not machine-readable in the profile catalog XML. The
	    builder therefore applies the following strategy:

	      1. A surrogate VARCHAR(100) 'id' column is used as the PRIMARY KEY,
	         identical in form to the CompoundType surrogate PK pattern.
	      2. A heuristic UNIQUE constraint is emitted across all required
	         (NOT NULL) non-surrogate columns. This is the strongest uniqueness
	         guard achievable without baking in class-specific knowledge. The
	         constraint may be wider than the true natural key (e.g. including
	         value columns alongside true key columns) but is never incorrect —
	         a wider-than-necessary UNIQUE prevents valid duplication attempts;
	         a missing UNIQUE permits silent semantic corruption.

	    The parallel Java JPA & C# EF builder (csharp-entity-framework.xsl) 
	    applies the identical detection logic and emits the matching surrogate 
	    [Key] and heuristic [Index(..., IsUnique=true)] annotations. Both builders 
	    share the same cimtool:has-mrid-ancestor concept to guarantee consistent 
	    output.

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
	
	<xsl:template name="type">
		<xsl:param name="name" select="@name"/>
		<xsl:text> </xsl:text>
		<xsl:choose>
			<xsl:when test="@xstype = 'string' and $name = 'mRID'">VARCHAR(100)</xsl:when>
			<xsl:when test="@xstype = 'string'">VARCHAR(255)</xsl:when>
			<xsl:when test="@xstype = 'normalizedString'">VARCHAR(255)</xsl:when>
			<xsl:when test="@xstype = 'token'">VARCHAR(255)</xsl:when>
			<!-- The 2,048-character limit is widely considered a safe maximum for URLs in 
			     practice, especially due to legacy compatibility with Internet Explorer. -->
			<xsl:when test="@xstype = 'anyURI' ">VARCHAR(2048)</xsl:when>
			<!-- short is a 16-bit signed integer -->
			<xsl:when test="@xstype = 'short'">SMALLINT</xsl:when>
			<!-- Below is a 32-bit signed integer -->
			<!-- INTEGER is a standard exact numeric type defined in the ANSI    -->
			<!-- SQL standard (SQL-92) as a 32-bit integer.                      -->
			<!-- Represents: whole numbers from -2,147,483,648 to +2,147,483,647 -->
			<!-- Equivalent to: NUMERIC(p, 0) with p roughly <= 10               -->
			<xsl:when test="@xstype = 'integer' or @xstype = 'int'">INTEGER</xsl:when>
			<!-- long is a 64-bit signed integer -->
			<xsl:when test="@xstype = 'long'">BIGINT</xsl:when>
			<!--  Binary encoded in base64 -->
			<xsl:when test="@xstype = 'base64Binary'">BLOB</xsl:when>
			<!--  Binary encoded in hex -->
			<xsl:when test="@xstype = 'hexBinary'">BLOB</xsl:when>
			<xsl:when test="@xstype = 'decimal'">DOUBLE PRECISION</xsl:when>
			<xsl:when test="@xstype = 'float'">DOUBLE PRECISION</xsl:when>
			<xsl:when test="@xstype = 'double'">DOUBLE PRECISION</xsl:when>
			<xsl:when test="@xstype = 'date'">DATE</xsl:when>
			<xsl:when test="@xstype = 'time'">TIME</xsl:when>
			<xsl:when test="@xstype = 'dateTime'">TIMESTAMP</xsl:when>
			<!-- Boolean columns use an INTEGER with a CHECK constraint for cross-RDBMS compatibility.
			     DEFAULT 0 (false) is the correct sentinel — an absent boolean defaults to false.
			     NOT NULL is intentionally omitted here; the notnull named template appends it
			     based on @minOccurs so that optional booleans (minOccurs=0) remain nullable
			     and required booleans (minOccurs=1) get exactly one NOT NULL. -->
			<xsl:when test="@xstype = 'boolean'">INTEGER DEFAULT 0 CHECK ("<xsl:value-of select="$name"/>" IN (0, 1))</xsl:when>
			<xsl:otherwise>VARCHAR(255)</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="notnull">
		<xsl:if test="@minOccurs > 0"> NOT NULL</xsl:if>
	</xsl:template>
 
	<xsl:template match="a:Catalog">
		<!--  the top level template  -->
		<document>
			<item>-- ==========================================================================================</item>
			<list indent="-- ">
				<item>ANSI-SQL-92 compliant schema for <xsl:value-of select="$envelope" /></item>
				<item>Generated by CIMTool https://cimtool.ucaiug.io</item>
				<item/>
				<item>Compound types are represented as separate 1:1 tables and enforced via</item>
				<item>a combination of UNIQUE on columns that reference Compound type tables</item>
				<item>and FK column constraint definitions to those tables.</item>
				<item/>
				<item>Though Compounds do not have an "mRID" and do not inherently represent</item>
				<item>persistent objects in the CIM (i.e. they do not inherit from IdentifiedObject)</item>
				<item>they are given a surrogate identifier "id" distinct from "mRID" for</item>
				<item>persistence purposes.</item>
				<item/>
				<item>Non-IdentifiedObject classes (e.g. CurveData, RegularTimePoint,</item>
				<item>NonlinearShuntCompensatorPoint, TapChangerTablePoint) also receive a</item>
				<item>surrogate "id" PRIMARY KEY for the same reason — they are collection-member</item>
				<item>or point-data classes with no natural single-column PK derivable from the</item>
				<item>profile catalog XML. A heuristic UNIQUE constraint is additionally emitted</item>
				<item>across all their required (NOT NULL) non-surrogate columns. See the</item>
				<item>cimtool:has-mrid-ancestor function documentation for full details.</item>
				<item/>
				<item>Tables that represent Compound types in the CIM are not normalized for</item>
				<item>the following reasons:</item>
				<item/>
			</list>
			<list indent="--   ">
				<item>Normalization, particularly to 3NF, typically requires:</item>
			</list>
			<list indent="--     ">
				<item>No repeating groups.</item>
				<item>No partial or transitive dependencies.</item>
				<item>Every non-key attribute must depend only on the whole key.</item>
				<item/>
			</list>
			<list indent="--   ">
				<item>But compound classes in the CIM domain:</item>
			</list>
			<list indent="--     ">
				<item>Do not have surrogate or natural primary keys derived from business rules.</item>
				<item>Are logically value objects: any change to any attribute means the value is distinct.</item>
				<item>Have implicit semantic atomicity — they're treated as indivisible chunks.</item>
				<item/>
			</list>
			<list indent="--   ">
				<item>Compound Value Object Design Concession</item>
				<item/>
				<item>CIM Compound types are treated as value objects — each compound row has</item>
				<item>exactly one owner: the specific parent column on the specific parent row</item>
				<item>that caused it to be created. Compound rows are NEVER shared between</item>
				<item>parent columns or between parent rows, even when the attribute content</item>
				<item>is identical. For example, if an Organisation has both a postalAddress</item>
				<item>and a streetAddress that happen to hold the same physical address, two</item>
				<item>distinct StreetAddress rows must still be inserted — each with its own</item>
				<item>unique UUID surrogate "id" — one owned exclusively by postalAddress and</item>
				<item>one owned exclusively by streetAddress.</item>
				<item/>
				<item>This is a deliberate design concession. It forgoes 3rd Normal Form for</item>
				<item>compound data in exchange for a simple, consistent ownership model where</item>
				<item>each compound row lives and dies with its single parent column instance.</item>
				<item>Consumers inserting instance data for compounds MUST always generate a</item>
				<item>unique UUID for each compound row and use that UUID as the FK value in</item>
				<item>the parent column. Reuse of a compound UUID across multiple parent columns</item>
				<item>or parent rows will violate the cascade delete constraints defined below</item>
				<item>and corrupt the ownership semantics of the schema.</item>
			</list>
			<list indent="--   ">
				<item>Compound Surrogate Key (UUID) Generation</item>
				<item/>
				<item>Each compound row requires a unique surrogate identifier assigned to its</item>
				<item>"id" column at insert time. This UUID serves as both the primary key of</item>
				<item>the compound row and the foreign key value stored in the parent column</item>
				<item>(e.g. Organisation.postalAddress). Because each compound row has exactly</item>
				<item>one owner, the UUID must be generated fresh for every compound insert —</item>
				<item>it must never be reused across parent columns or parent rows.</item>
				<item/>
				<item>UUID generation is intentionally left to application-level code. ANSI</item>
				<item>SQL-92 defines no standard mechanism for generating UUIDs or other unique</item>
				<item>identifiers as column defaults, and all available approaches are</item>
				<item>vendor-specific (e.g. gen_random_uuid() in PostgreSQL, NEWID() in SQL</item>
				<item>Server, UUID() in MySQL). Embedding any of these would break the</item>
				<item>cross-RDBMS portability that this generated schema is designed to preserve.</item>
				<item/>
				<item>Application code should generate UUIDs using the standard library</item>
				<item>appropriate to the implementation language before issuing any insert</item>
				<item>statements, for example:</item>
				<item/>
				<item>  Java:    java.util.UUID.randomUUID().toString()</item>
				<item>  C#:      System.Guid.NewGuid().ToString()</item>
				<item>  Python:  str(uuid.uuid4())</item>
				<item>  Node.js: crypto.randomUUID()</item>
				<item/>
				<item>The UUID should be assigned to the compound row's "id" column and</item>
				<item>simultaneously written into the parent row's compound FK column within</item>
				<item>the same transaction, ensuring consistency.</item>
				<item/>
				<item>Note that a custom backend-specific SQL builder could be implemented</item>
				<item>alongside this one that targets a particular RDBMS and includes native</item>
				<item>UUID generation as a column default (e.g. DEFAULT gen_random_uuid()).</item>
				<item>Such a builder would sacrifice portability in exchange for removing the</item>
				<item>UUID generation burden from application code entirely.</item>
				<item/>
				<item>If utilizing the jpa-rdfs or csharp-ef-rdfs CIMTool builders, this concern</item>
				<item>is addressed directly in the code they generate. UUID assignment for compound</item>
				<item>surrogate keys is handled automatically as a cross-cutting concern within</item>
				<item>those builders, and no additional application-level UUID generation code is</item>
				<item>required for compound inserts when using those generated artifacts.</item>
			</list>
			<item>-- ==========================================================================================</item>
			<item>--</item>
		    <xsl:apply-templates/>
			<item/>
			<item>------------------------------------------------------------------------------</item>
		    <item>-- Inheritance constraint definitions</item>
		    <item>------------------------------------------------------------------------------</item>
		    <xsl:apply-templates mode="inheritance-constraints"/>
			<item/>
			<item>------------------------------------------------------------------------------</item>
		    <item>-- Standard foreign key constraint definitions</item>
		    <item>------------------------------------------------------------------------------</item>
		    <xsl:apply-templates mode="constraints"/>
		    <item/>
		    <item>------------------------------------------------------------------------------</item>
		    <item>-- Heuristic uniqueness constraints for non-IdentifiedObject classes</item>
		    <item>------------------------------------------------------------------------------</item>
		    <item/>
		    <list indent="-- ">
				<item>The following UNIQUE constraints are emitted for CIM classes that do not</item>
				<item>inherit from IdentifiedObject and therefore carry a surrogate "id" PK rather</item>
				<item>than a natural "mRID" PK. Because the profile catalog XML does not encode</item>
				<item>which subset of columns forms the true natural composite key (that information</item>
				<item>exists only in the IEC specification prose), these constraints span all</item>
				<item>required (NOT NULL) non-surrogate columns as a heuristic guard. The</item>
				<item>constraint may be wider than the true natural key but is never incorrect —</item>
				<item>a wider UNIQUE prevents valid duplicate insertion; a missing UNIQUE permits</item>
				<item>silent semantic corruption.</item>
			</list>
		    <xsl:apply-templates mode="unique-constraints"/>
		    <item/>
		    <item>------------------------------------------------------------------------------</item>
		    <item>-- Cascade delete foreign key constraint definitions (for Compounds)</item>
		    <item>------------------------------------------------------------------------------</item>
		    <item/>
		    <list indent="-- ">
				<item>The following cascade delete constraints implement the compound value object</item>
				<item>ownership model described in the file header. Each constraint takes the form:</item>
				<item/>
				<item>  ALTER TABLE &lt;CompoundType&gt; ADD CONSTRAINT ... FOREIGN KEY ("id")</item>
				<item>      REFERENCES &lt;ParentTable&gt; (&lt;compoundColumn&gt;) ON DELETE CASCADE;</item>
				<item/>
				<item>This reverse-reference pattern means: when a parent row is deleted, any</item>
				<item>compound child row whose "id" matches the parent's compound column value</item>
				<item>is automatically deleted. Because each compound row has exactly one owner</item>
				<item>(enforced by the unique UUID insertion directive above), this cascade</item>
				<item>correctly and completely cleans up all compound child rows when their</item>
				<item>parent is deleted — no application-level programmatic deletion is required.</item>
				<item/>
				<item>Known DBMS constraint engine limitation: the DBMS enforces each cascade FK</item>
				<item>constraint globally across all rows in the compound table. It has no awareness</item>
				<item>of the per-row ownership convention. Consumers who violate the unique UUID</item>
				<item>insertion directive — for example by reusing a compound row UUID in more than</item>
				<item>one parent column — will encounter FK constraint violations at insert time,</item>
				<item>since the DBMS will check the reused UUID against every cascade constraint</item>
				<item>on the compound table simultaneously. This is by design: the constraint</item>
				<item>violation serves as the enforcement mechanism for the ownership model.</item>
			</list>
		    <xsl:apply-templates mode="cascade-constraints"/>
		    <item/>
		    <item>------------------------------------------------------------------------------</item>
		    <item>-- Foreign key column indexes for optimized queries and joins</item>
		    <item>------------------------------------------------------------------------------</item>
		    <item/>
		    <xsl:apply-templates mode="fk-indexes"/>
		</document>
	</xsl:template>
	<xsl:template match="a:Root|a:ComplexType|a:CompoundType">
		<!-- 
			a table
		     PK column selection:
		       - CompoundType and #compound-stereotyped Root  ->  surrogate "id"
		       - ComplexType NOT in the IdentifiedObject hierarchy  ->  surrogate "id"
		         (same pattern as Compound; see cimtool:has-mrid-ancestor documentation)
		       - All other ComplexType/Root  ->  natural "mRID" from IdentifiedObject 
		-->
		<item/>
		<xsl:call-template name="annotate" />
		<item>CREATE TABLE <xsl:call-template name="ident"/></item>
		<list begin="(" indent="    " delim="," end=");">
		    <item><xsl:choose>
		        <xsl:when test="self::a:CompoundType or (self::a:Root and a:Stereotype[contains(., '#compound')])"> "id" </xsl:when>
		        <xsl:when test="self::a:ComplexType and not(cimtool:has-mrid-ancestor(.))"> "id" </xsl:when>
		        <xsl:when test="self::a:Root and not(a:Stereotype[contains(., '#compound')]) and not(cimtool:has-mrid-ancestor(.))"> "id" </xsl:when>
		        <xsl:otherwise> "mRID" </xsl:otherwise>
		    </xsl:choose><xsl:value-of select="$uuidType"/> PRIMARY KEY</item>
		    <xsl:apply-templates/>
		</list>
	</xsl:template>
	
	<xsl:template match="a:EnumeratedType">
		<!-- a reference table for an enumeration -->
		<item/>
		<xsl:call-template name="annotate" />
		<item>
		    CREATE TABLE <xsl:call-template name="ident"/>
		    ( "name" VARCHAR(100) PRIMARY KEY );
		</item>    

		<xsl:variable name="name" select="@name"/>
		<xsl:for-each select="a:EnumeratedValue">
			<!-- inserts one value into a reference table -->
			<xsl:call-template name="annotate" />
			<item>
				INSERT INTO 
    		    <xsl:call-template name="ident">
    			    <xsl:with-param name="name" select="$name"/>
    		    </xsl:call-template> 
			    ( "name" ) VALUES ( '<xsl:value-of select="@name"/>' );
			</item>
		</xsl:for-each>
	</xsl:template>
	
	<!-- ============================================================================================================== -->
	<!--    The reason we have the various filters (e.g. a:Reference[not(a:Stereotype[contains(., '#enumeration')])]    -->
	<!--    on any templates that have a:Reference is that we need to filter out (or include) where the end-user has    -->
	<!--    added an attribute of a type that is either an enumeration or a compound, but for which they neglected to   -->
	<!--    perform a deep copy when adding it to the profile. In such cases the attribute will appear in the interim   --> 
	<!--    XML format for the profile as an a:Reference with either an #enumeration or #compound stereotype. Therefore -->
	<!--    we either include or exclude as needed depending on the template.                                           -->
	<!-- ============================================================================================================== -->	
	
	<xsl:template match="a:Instance|a:Reference[not(a:Stereotype[contains(., '#enumeration')])]|a:Compound">
		<xsl:if test="not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID'">
			<!-- a foreign key column -->
			<decorate>
				<xsl:call-template name="annotate" />
				<xsl:choose>
					<xsl:when test="self::a:Compound or (self::a:Reference and a:Stereotype[contains(., '#compound')])">
						<item>-- FK column reference to the table representing the "<xsl:value-of select="@type"/>" compound</item>
					</xsl:when>
					<xsl:otherwise>
						<item>-- FK column reference to table representing the "<xsl:value-of select="@type"/>" class</item>
					</xsl:otherwise>
				</xsl:choose>
				<item>
					<xsl:call-template name="ident"/>
					<xsl:text> </xsl:text>
					<xsl:value-of select="$uuidType"/>
					<xsl:if test="self::a:Compound"> 
						UNIQUE 
					</xsl:if>
					<xsl:call-template name="notnull"/> 
				</item>
			</decorate>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Root[a:SuperType]|a:ComplexType[a:SuperType]" mode="inheritance-constraints" >
		<item/>
		<item>-- Inheritance subclass-superclass constraint for table "<xsl:value-of select="@name"/>"</item>
		<xsl:apply-templates mode="inheritance-constraints"/>
	</xsl:template>
	
	<xsl:template match="a:SuperType" mode="inheritance-constraints">
		<!-- Determine the PK column name for both sides of the inheritance FK.
		     For classes in the IdentifiedObject hierarchy the PK is "mRID".
		     For non-IdentifiedObject classes (e.g. TapChangerTablePoint and its
		     subclasses) the PK is the surrogate "id" — both the child FK column
		     and the parent referenced column must use the same name. -->
		<xsl:variable name="pkCol">
			<xsl:choose>
				<xsl:when test="cimtool:has-mrid-ancestor(..)">mRID</xsl:when>
				<xsl:otherwise>id</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<item>
		    ALTER TABLE 
			<xsl:call-template name="ident">
		        <xsl:with-param name="name" select="../@name"/>
			</xsl:call-template>
		    ADD FOREIGN KEY ( "<xsl:value-of select="$pkCol"/>" ) REFERENCES 
		    <xsl:call-template name="ident"/>
		    ( "<xsl:value-of select="$pkCol"/>" );
		</item> 
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType|a:CompoundType" mode="constraints" >
		<xsl:if test="a:Instance[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']
					 |a:Reference[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']
					 |a:Compound[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']">
			<item/>
			<item>-- Foreign keys for table "<xsl:value-of select="@name"/>"</item>
			<xsl:apply-templates mode="constraints"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType|a:CompoundType" mode="cascade-constraints" >
		<xsl:if test="a:Instance[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']
					 |a:Reference[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']
					 |a:Compound[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']">
			<item/>
			<item>-- Cascade deletes for compounds referenced in table "<xsl:value-of select="@name"/>"</item>
			<xsl:apply-templates mode="cascade-constraints"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Root|a:ComplexType|a:CompoundType" mode="fk-indexes" >
		<xsl:if test="a:Instance[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']
					 |a:Reference[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']
					 |a:Compound[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID']">
			<xsl:apply-templates mode="fk-indexes"/>
		</xsl:if>
	</xsl:template>
	
	<!--
	    FK constraint template for Instance, Reference (non-enumeration), and Compound
	    associations. Determines which PK column to reference on the target table:
	
	      1. Compound / #compound-stereotyped Reference  →  "id"
	         The target is a CompoundType which uses a surrogate "id" PK.
	
	      2. Non-IdentifiedObject Root or ComplexType  →  "id"
	         The target is a concrete or abstract class that does not inherit from
	         IdentifiedObject (detected via cimtool:has-mrid-ancestor on the element
	         resolved by @type). These classes also use a surrogate "id" PK.
	         Example: DiagramObjectPoint referencing DiagramObjectGluePoint.
	
	      3. All other targets  →  "mRID"
	         The target is in the IdentifiedObject hierarchy and uses the natural
	         "mRID" primary key.
	-->
	<xsl:template match="a:Instance|a:Reference[not(a:Stereotype[contains(., '#enumeration')])]|a:Compound" mode="constraints">
		<xsl:if test="not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID'">
			<item>
				ALTER TABLE
				<xsl:call-template name="ident">
					<xsl:with-param name="name" select="../@name"/>
				</xsl:call-template>
				ADD FOREIGN KEY ( <xsl:call-template name="ident"/> ) 
				REFERENCES   
				<xsl:call-template name="ident">
					<xsl:with-param name="name" select="@type"/>
				</xsl:call-template> 
				<xsl:choose>
					<!-- Case 1: Compound target — we use a surrogate "id" PK -->
					<xsl:when test="self::a:Compound or (self::a:Reference and a:Stereotype[contains(., '#compound')])">
						( "id" );
					</xsl:when>
					<!-- Case 2: Non-IdentifiedObject target — therefore we use a surrogate "id" PK -->
					<xsl:when test="
						let $refName    := string(@type),
						    $refElement := (root(.)//a:Root[@name = $refName] |
						                    root(.)//a:ComplexType[@name = $refName])[1]
						return $refElement and not(cimtool:has-mrid-ancestor($refElement))
					">
						( "id" );
					</xsl:when>
					<!-- Case 3: IdentifiedObject-hierarchy target — natural "mRID" PK -->
					<xsl:otherwise>
						( "mRID" );
					</xsl:otherwise>
				</xsl:choose>
			</item> 
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Compound|a:Reference[a:Stereotype[contains(., '#compound')]]" mode="cascade-constraints">
		<xsl:if test="not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID'">
			<xsl:variable name="fkTable" select="@type"/>
			<xsl:variable name="cascadeColumn" select="../@name"/>
			<xsl:variable name="attrName" select="@name"/>
			<item>
				ALTER TABLE
				<xsl:call-template name="ident">
					<xsl:with-param name="name" select="@type"/>
				</xsl:call-template> 
				ADD CONSTRAINT <xsl:value-of select="concat('fk_', $fkTable, '_', $cascadeColumn, '_', $attrName)"/> 
				FOREIGN KEY ( "id" ) REFERENCES 
				<xsl:call-template name="ident">
					<xsl:with-param name="name" select="../@name"/>
				</xsl:call-template> 
				( <xsl:call-template name="ident"/> )
				ON DELETE CASCADE;
			</item> 
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference[not(a:Stereotype[contains(., '#enumeration')])]|a:Compound" mode="fk-indexes">
		<xsl:if test="not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID'">
			<xsl:variable name="fkTable" select="../@name"/>
			<xsl:variable name="indexedColumn" select="@name"/>
			<item>
				CREATE INDEX 
				<xsl:value-of select="concat('ix_', $fkTable, '_', translate($indexedColumn, ' ', '_'))"/> 
				ON 
				<xsl:call-template name="ident">
					<xsl:with-param name="name" select="../@name"/>
				</xsl:call-template> 
				( <xsl:call-template name="ident"/> ); 
			</item>
		</xsl:if>
	</xsl:template>

	<xsl:template match="a:SimpleType|a:PrimitiveType">
		<!-- We do nothing for these types other than simply consume and ignore them. -->
	</xsl:template>
		
	<xsl:template match="a:Enumerated|a:Reference[a:Stereotype[contains(., '#enumeration')]]">
		<!-- a foreign key column for a reference table -->
		<decorate>
			<xsl:call-template name="annotate" />
			<item>-- FK column reference to table representing the "<xsl:value-of select="@type"/>" enumeration</item>
			<item>
				<xsl:call-template name="ident"/>
				VARCHAR(100)
				<xsl:call-template name="notnull"/>
			</item>
		</decorate>
	</xsl:template>
	
	<xsl:template match="a:Enumerated|a:Reference[a:Stereotype[contains(., '#enumeration')]]" mode="constraints">
		<item>
			ALTER TABLE
			<xsl:call-template name="ident">
				<xsl:with-param name="name" select="../@name"/>
			</xsl:call-template>
			ADD FOREIGN KEY ( <xsl:call-template name="ident"/> ) 
			REFERENCES 
   			<xsl:call-template name="ident">
				<xsl:with-param name="name" select="@type"/>
			</xsl:call-template> 
			( "name" );
		</item> 
	</xsl:template>

	<xsl:template match="a:Simple|a:Domain">
		<!-- a simple column  -->
		<xsl:if test="not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1 and @name != 'mRID'">
			<decorate>
				<xsl:call-template name="annotate" />
				<item>
					<xsl:call-template name="ident"/>
					<xsl:call-template name="type"/>
					<xsl:call-template name="notnull"/>
				</item>
			</decorate>
		</xsl:if>
	</xsl:template>

	<xsl:template name="annotate">
		<!--  generate and annotation -->
		<list indent="-- ">
			<xsl:apply-templates mode="annotate"/>
		</list>
	</xsl:template>

	<xsl:template match="a:Comment|a:Note" mode="annotate">
		<!--  generate human readable annotation -->
		<wrap width="70">
			<xsl:value-of select="."/>
		</wrap>
	</xsl:template>

	<xsl:template match="text()">
		<!--  dont pass text through  -->
	</xsl:template>

	<xsl:template match="node()" mode="inheritance-constraints">
		<!-- dont pass any defaults in inheritance-constraints mode -->
	</xsl:template>
	
	<xsl:template match="node()" mode="constraints">
		<!-- dont pass any defaults in constraints mode -->
	</xsl:template>
	
	<xsl:template match="node()" mode="cascade-constraints">
		<!-- dont pass any defaults in constraints mode -->
	</xsl:template>
	
	<xsl:template match="node()" mode="fk-indexes">
		<!-- dont pass any defaults in fk-indexes mode -->
	</xsl:template>

	<!--
	    unique-constraints mode

	    Emits a heuristic ALTER TABLE ... ADD UNIQUE (...) statement for every
	    a:ComplexType that does NOT belong to the IdentifiedObject hierarchy
	    (detected via cimtool:has-mrid-ancestor).

	    These classes carry a surrogate "id" PRIMARY KEY. Because the profile
	    catalog XML does not identify which subset of columns forms the true natural
	    composite key, the UNIQUE constraint spans all required (NOT NULL)
	    non-surrogate columns:
	      - a:Simple / a:Domain with @minOccurs > 0 (required scalar attributes)
	      - a:Instance / a:Reference with @minOccurs > 0 and single-valued
	        (required FK columns, whether pointing at IdentifiedObject or
	         enumeration tables)
	    a:Compound children are excluded — compound FK columns are already UNIQUE
	    by the existing compound surrogate mechanism.

	    The constraint may span more columns than the true natural key (e.g. value
	    columns alongside true key columns) but is never wrong. See the
	    cimtool:has-mrid-ancestor documentation for the full design rationale.

	    a:Root elements that are concrete (non-compound, non-IdentifiedObject)
	    are now also included — e.g. CurveData, RegularTimePoint, and
	    NonlinearShuntCompensatorPoint are a:Root in the profile catalog XML
	    (concrete classes) while TapChangerTablePoint is a:ComplexType (abstract).
	    Both cases require the same surrogate id + heuristic UNIQUE treatment.
	-->
	<xsl:template match="a:ComplexType|a:Root" mode="unique-constraints">
		<xsl:if test="not(cimtool:has-mrid-ancestor(.)) and not(a:Stereotype[contains(., '#compound')])">
			<!-- Collect required scalar attribute column names -->
			<xsl:variable name="scalar-cols" as="xs:string*"
				select="for $a in (a:Simple|a:Domain)
							[@minOccurs > 0]
							[@name != 'mRID']
							[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1]
						return concat('&quot;', translate($a/@name, ' ', '_'), '&quot;')"/>
			<!-- Collect required FK column names (Instance, Reference, and Enumerated) -->
			<xsl:variable name="fk-cols" as="xs:string*"
				select="for $a in (a:Instance
								  |a:Reference[not(a:Stereotype[contains(., '#enumeration')])]
								  |a:Enumerated)
							[@minOccurs > 0]
							[not(@maxOccurs = 'unbounded') and @maxOccurs &lt;= 1]
						return concat('&quot;', translate($a/@name, ' ', '_'), '&quot;')"/>
			<xsl:variable name="all-cols" as="xs:string*" select="($scalar-cols, $fk-cols)"/>
			<xsl:if test="count($all-cols) gt 0">
				<item/>
				<item>-- Heuristic uniqueness constraint for non-IdentifiedObject table "<xsl:value-of select="@name"/>"</item>
				<item>ALTER TABLE <xsl:call-template name="ident"/> ADD UNIQUE (<xsl:value-of select="string-join($all-cols, ', ')"/>);</item>
			</xsl:if>
		</xsl:if>
	</xsl:template>

	<xsl:template match="node()" mode="unique-constraints">
		<!-- dont pass any defaults in unique-constraints mode -->
	</xsl:template>

	<xsl:template match="node()" mode="annotate">
		<!-- dont pass any defaults in annotate mode -->
	</xsl:template>

</xsl:stylesheet>
