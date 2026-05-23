<?xml version="1.0" encoding="UTF-8"?>
<!--
  Copyright 2024 UCAIug

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
<xsl:stylesheet exclude-result-prefixes="a" version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:a="http://langdale.com.au/2005/Message#" xmlns="http://langdale.com.au/2009/Indent" xmlns:map="http://www.w3.org/2005/xpath-functions/map" xmlns:fn="http://www.w3.org/2005/xpath-functions" xmlns:cimtool="http://cimtool.ucaiug.io/functions">

    <xsl:output xmlns:xalan="http://xml.apache.org/xslt" method="xml" omit-xml-declaration="no" indent="yes" encoding="utf-8" xalan:indent-amount="4" />
    <xsl:param name="copyright-single-line" />
	<xsl:param name="version"/>
	<xsl:param name="baseURI"/>
	<xsl:param name="envelope">Profile</xsl:param>
	<xsl:variable name="package_prefix" select="fn:concat(cimtool:baseuri-to-package($baseURI), '.', fn:lower-case(fn:replace($envelope, '- ', '_')))"/>
	<!-- The following <xsl:text> element is our newline representation where needed. -->
    <xsl:variable name="newline"><xsl:text>
</xsl:text></xsl:variable>
	
	<!--
	════════════════════════════════════════════════════════════════════════════════
	BEGIN: NUMERIC CHARACTER REFERENCE AND ESCAPING UTILITIES
	════════════════════════════════════════════════════════════════════════════════

	This group of functions handles conversion between hexadecimal strings, numeric
	character references (HTML/XML entities), and escape sequences. These are
	used to properly encode Unicode text when generating documentation or processing 
	XML entities.
	-->
	
	<!-- Variable used to process unicode with hex prefixes. -->
	<xsl:variable name="hex-digits" as="xs:string">0123456789ABCDEF</xsl:variable>
		
	<!-- 
		Function: cimtool:hex-char-value
		Purpose: Converts a single hexadecimal character (0-9, A-F) to its integer value (0-15)
		Parameters: 
			$ch (xs:string) - Single hexadecimal character (case-insensitive)
		Returns: xs:integer - The numeric value (0-15) of the hex character
		Algorithm: Converts character to uppercase, then finds its position in the hex-digits string "0123456789ABCDEF"
		Examples: 
			cimtool:hex-char-value('0') returns 0
			cimtool:hex-char-value('A') returns 10
			cimtool:hex-char-value('F') returns 15
			cimtool:hex-char-value('a') returns 10 (case-insensitive)
	-->
	<xsl:function name="cimtool:hex-char-value" as="xs:integer">
		<xsl:param name="ch" as="xs:string"/>
		<xsl:variable name="u" select="upper-case($ch)"/>
		<!-- Index of the character in 0..15 -->
		<xsl:sequence select="string-length(substring-before($hex-digits, $u))"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:hex-to-int
		Purpose: Converts a hexadecimal string to its decimal integer equivalent
		Parameters: 
			$s (xs:string) - Hexadecimal string (e.g., "1A2F", "FF")
		Returns: xs:integer - The decimal integer value
		Algorithm: Recursive conversion using positional values (base-16). For each position, multiplies the digit value by 16^position and sums all positions. Processes right-to-left: rightmost digit + 16 × (remaining digits).
		Examples: 
			cimtool:hex-to-int('0') returns 0
			cimtool:hex-to-int('FF') returns 255
			cimtool:hex-to-int('1A2F') returns 6703
			cimtool:hex-to-int('CAFE') returns 51966
		Dependencies: cimtool:hex-char-value() function
	-->
	<xsl:function name="cimtool:hex-to-int" as="xs:integer">
		<xsl:param name="s" as="xs:string"/>
	
		<xsl:variable name="result" select="
			if (string-length($s) = 0)
			then 0
			else cimtool:hex-char-value(substring($s, string-length($s), 1))
				 + 16 * cimtool:hex-to-int(substring($s, 1, string-length($s) - 1))
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<!-- ASCII range we want to pass through unchanged: space (32) to tilde (126) -->
	<xsl:variable name="ascii-min" as="xs:integer">32</xsl:variable>
	<xsl:variable name="ascii-max" as="xs:integer">126</xsl:variable>
	
	<!-- 
		Function: cimtool:json-escape
		Purpose: Escapes text for use in JSON strings by escaping special characters according to JSON specification
		Parameters: 
			$text (xs:string) - Text to escape for JSON
		Returns: xs:string - JSON-escaped text
		Algorithm: Escapes backslashes, double quotes, and control characters. For non-ASCII characters, either passes through as UTF-8 (recommended) or converts to JSON Unicode escapes (\uHHHH format with 4 hex digits).
		Examples: 
			cimtool:json-escape('Hello "World"') returns 'Hello \"World\"'
			cimtool:json-escape('Line1
	Line2') returns 'Line1\nLine2'
			cimtool:json-escape('C:\path\file') returns 'C:\\path\\file'
			cimtool:json-escape('Café') returns 'Café' (UTF-8 passthrough, valid in JSON)
		Notes: Modern JSON parsers accept UTF-8 directly, so non-ASCII characters typically don't need escaping. However, backslashes, quotes, and control characters MUST be escaped.
	-->
	<xsl:function name="cimtool:json-escape" as="xs:string">
		<xsl:param name="text" as="xs:string"/>
		
		<!-- Step 1: Escape backslashes (MUST be first!) -->
		<xsl:variable name="step1" select="replace($text, '\\', '\\\\')"/>
		
		<!-- Step 2: Escape double quotes -->
		<xsl:variable name="step2" select="replace($step1, '&quot;', '\\&quot;')"/>
		
		<!-- Step 3: Escape control characters -->
		<xsl:variable name="step3" select="replace($step2, '&#x0A;', '\\n')"/>  <!-- newline -->
		<xsl:variable name="step4" select="replace($step3, '&#x0D;', '\\r')"/>  <!-- carriage return -->
		<xsl:variable name="step5" select="replace($step4, '&#x09;', '\\t')"/>  <!-- tab -->
		
		<xsl:sequence select="$step5"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:unescape-numeric
		Purpose: Decodes HTML/XML numeric character references (both hexadecimal and decimal) back to their Unicode character equivalents
		Parameters: 
			$text (xs:string) - Text containing numeric character references
		Returns: xs:string - Text with all numeric entities converted to Unicode characters
		Numeric Character Reference Formats:
			Hexadecimal: &#xHHHH; (e.g., &#xE9; for é)
			Decimal: &#NNNN; (e.g., &#233; for é)
		Algorithm: Two-pass processing. Pass 1: Decode hexadecimal entities (&#xHHHH;) using regex, extract hex digits, convert to codepoint, convert to character. Pass 2: Decode decimal entities (&#NNNN;) using regex, extract decimal digits, convert to codepoint, convert to character.
		Examples: 
			cimtool:unescape-numeric('Caf&#xE9;') returns 'Café'
			cimtool:unescape-numeric('Caf&#233;') returns 'Café'
			cimtool:unescape-numeric('&#x4E16;&#x754C;') returns '世界'
			cimtool:unescape-numeric('&lt;tag&gt;') returns '&lt;tag&gt;' (named entities unchanged)
		Dependencies: cimtool:hex-to-int() function for hexadecimal conversion
		Notes: Handles both hex and decimal formats. Named entities (&lt;, &gt;, &amp;, etc.) are NOT decoded. Case-insensitive for hex digits.
		Use Cases: Processing XML/HTML content with encoded characters, converting entity-encoded text to readable Unicode, pre-processing before RTF generation
		Relationship: Inverse operation of XML serialization entity encoding. Often used before cimtool:json-escape() in pipelines: XML entities → Unicode → RTF escapes
	-->
	<xsl:function name="cimtool:unescape-numeric" as="xs:string">
		<xsl:param name="text" as="xs:string"/>
	
		<!-- First, decode hex entities: &#xHHHH; -->
		<xsl:variable name="after-hex">
			<xsl:analyze-string select="$text" regex="&amp;#x([0-9A-Fa-f]+);">
				<xsl:matching-substring>
					<xsl:variable name="hex" select="regex-group(1)"/>
					<xsl:variable name="cp" select="cimtool:hex-to-int($hex)"/>
					<xsl:value-of select="fn:codepoints-to-string($cp)"/>
				</xsl:matching-substring>
				<xsl:non-matching-substring>
					<xsl:value-of select="."/>
				</xsl:non-matching-substring>
			</xsl:analyze-string>
		</xsl:variable>
	
		<!-- Then, decode decimal entities: &#NNNN; -->
		<xsl:variable name="after-dec">
			<xsl:analyze-string select="string($after-hex)" regex="&amp;#([0-9]+);">
				<xsl:matching-substring>
					<xsl:variable name="cp" select="xs:integer(regex-group(1))"/>
					<xsl:value-of select="fn:codepoints-to-string($cp)"/>
				</xsl:matching-substring>
				<xsl:non-matching-substring>
					<xsl:value-of select="."/>
				</xsl:non-matching-substring>
			</xsl:analyze-string>
		</xsl:variable>
	
		<xsl:sequence select="string($after-dec)"/>
	</xsl:function>

	<!--
	════════════════════════════════════════════════════════════════════════════════
	END: NUMERIC CHARACTER REFERENCE AND ESCAPING UTILITIES
	════════════════════════════════════════════════════════════════════════════════
	-->
	
	<!-- 
		Function: cimtool:baseuri-to-package
		Purpose: Converts a baseURI into a package name by reversing the order of dot-separated tokens in a string
		Parameters: 
			$baseURI - The baseURI to reverse (e.g., "a.b.c" becomes "c.b.a")
		Returns: xs:string - The reversed text
		Example: cimtool:convert-baseuri-to-package('https://ap-voc.cim4.eu/SecurityAnalysisResult/2.4#') returns 'eu.cim4.ap-voc'
	-->
	<xsl:function name="cimtool:baseuri-to-package" as="xs:string">
		<xsl:param name="text" as="xs:string"/>
		<xsl:variable name="package-text" as="xs:string">
			<xsl:choose>
				<xsl:when test="contains($text, '://')">
					<xsl:value-of select="fn:substring-before(fn:substring-after(fn:lower-case(fn:replace($text, '-', '_')),'://'),'/')"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$text"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:choose>
			<xsl:when test="contains($text, '.')">
				<!-- Recursive call for the part after the first dot -->
				<xsl:variable name="reversed-tail" select="cimtool:baseuri-to-package(substring-after($package-text, '.'))"/>
				<!-- Concatenate: reversed tail + dot + first token -->
				<xsl:sequence select="concat($reversed-tail, '.', substring-before($package-text, '.'))"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- Base case: no more dots, return the text as-is -->
				<xsl:sequence select="$package-text"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<!-- 
		Function: cimtool:fully-qualified-class
		Purpose: Generates a fully qualified class name with package prefix
		Parameters: 
			$element - The element (a:Root or other) to generate the fully qualified class name from
			$package-prefix - The package prefix to prepend to the package/class name
		Returns: xs:string - The fully qualified class name
	-->
	<xsl:function name="cimtool:fully-qualified-class" as="xs:string">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="package-prefix" as="xs:string"/>
		
		<!-- Determine the class name based on element type -->
		<xsl:variable name="class-name" select="
			if ($element/self::a:Root)
			then 
				concat(
					upper-case(substring(substring-after($element/@baseClass, '#'), 1, 1)),
					substring(substring-after($element/@baseClass, '#'), 2)
				)
			else 
				concat(
					upper-case(substring($element/@name, 1, 1)),
					substring($element/@name, 2)
				)
		"/>
		
		<!-- Build the fully qualified name -->
		<xsl:variable name="result" select="
			if ($element/@package)
			then concat($package-prefix, '.', lower-case($element/@package), '.', $class-name)
			else concat($package-prefix, '.', $class-name)
		"/>

		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:package-name
		Purpose: Generates a package name based on the element's @package attribute
		Parameters: 
			$element - The element to get the package name from
			$package-prefix - The prefix to prepend to the package name
		Returns: xs:string - The full package name
	-->
	<xsl:function name="cimtool:package-name" as="xs:string">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="package-prefix" as="xs:string"/>
		
		<xsl:variable name="result" select="
			if ($element/@package)
			then concat($package-prefix, '.', lower-case($element/@package))
			else $package-prefix
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>

	<!-- 
		Function: cimtool:to-avro-type
		Purpose: Maps XSD types to Apache Avro primitive types or logical types
		Parameters: 
			$xstype - The XSD type name to map
		Returns: xs:string - The Avro type representation (as a JSON string)
		
		Note: int = 32-bit, long = 64-bit; prefer long for timestamps and ids.
		
		Special cases:
		- duration: Uses custom logicalType because Avro has no built-in duration type (months/years vary in length)
		- gMonthDay: Uses a record structure with month and day fields because Avro has no built-in equivalent
	-->
	<xsl:function name="cimtool:get-avro-type" as="xs:string">
		<xsl:param name="xstype"/>
		
		<!-- int = 32-bit, long = 64-bit; prefer long for timestamps and ids. -->
		<xsl:variable name="avro-type-string">
			<xsl:choose>
				<xsl:when test="$xstype = 'string'">"string"</xsl:when>
				<xsl:when test="$xstype = 'normalizedString'">"string"</xsl:when>
				<xsl:when test="$xstype = 'token'">"string"</xsl:when>
				<xsl:when test="$xstype = 'NMTOKEN'">"string"</xsl:when>
				<xsl:when test="$xstype = 'anyURI'">"string"</xsl:when>
				<xsl:when test="$xstype = 'NCName'">"string"</xsl:when>
				<xsl:when test="$xstype = 'Name'">"string"</xsl:when>
				<xsl:when test="$xstype = 'integer' or $xstype = 'int' or $xstype = 'short'">"int"</xsl:when>
				<xsl:when test="$xstype = 'long'">"long"</xsl:when>
				<xsl:when test="$xstype = 'float'">"float"</xsl:when>
				<xsl:when test="$xstype = 'double' or $xstype = 'decimal' or $xstype = 'number'">"double"</xsl:when>
				<xsl:when test="$xstype = 'boolean'">"boolean"</xsl:when>
				<xsl:when test="$xstype = 'dateTime'">{"type": "long", "logicalType": "timestamp-millis"}</xsl:when>
				<xsl:when test="$xstype = 'date'">{"type": "int", "logicalType": "date"}</xsl:when>
				<xsl:when test="$xstype = 'time'">{"type": "int", "logicalType": "time-millis"}</xsl:when>
				<!--
				Note that Apache Avro does not have a built-in logicalType equivalent to XSD duration (xs:duration).
	
				Why? Avro's logical types are deliberately narrow: they cover dates, times, timestamps, decimal, and
				a few UUID-like identifiers. There's nothing built-in for:
	
					- ISO-8601 durations like "P3Y6M4DT12H30M5S"
					- Arbitrary time spans with months/years where calendar context matters
	
				That's because durations in XSD (and ISO-8601) are not fixed units — months and years vary in length — so
				they can't be represented unambiguously as a simple integer count of milliseconds or days. Avro's philosophy
				is: if a type needs more context than the stored value can provide, "it stays in application space".
	
				Here a custom logicalType is used that is not officially recognized by Avro — but for which we can enforce
				meaning in our tooling.
				-->
				<xsl:when test="$xstype = 'duration'">{"type": "string", "logicalType": "iso8601-duration"}</xsl:when>
				<!--
				Note that Apache Avro does not have a built-in logicalType equivalent to XSD duration (xs:gMonthDay).
				Therefore the below custom logicalType convention for representing the 'gMonthDay'.
				-->
				<xsl:when test="$xstype = 'gMonthDay'">{"type": {"type": "record", "name": "MonthDay", "fields": [{"name": "month", "type": "int"},{"name": "day", "type": "int"}]}}</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$xstype"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<!-- Convert the string variable to a sequence and return it -->
		<xsl:sequence select="$avro-type-string"/>
	</xsl:function>
	
	<!-- Single parameter version (starts count at 0) -->
	<xsl:function name="cimtool:count-fields" as="xs:integer">
		<xsl:param name="element" as="element()"/>
		<xsl:sequence select="cimtool:count-fields($element, 0)"/>
	</xsl:function>
	
	<!-- Two parameter version (for recursion) -->
	<xsl:function name="cimtool:count-fields" as="xs:integer">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="count" as="xs:integer"/>
		
		<xsl:variable name="total-count" select="
			$count + count($element/(a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice))
		"/>
		
		<xsl:variable name="result-count" select="
			if ($element/a:SuperType) then
				let $baseClass := $element/a:SuperType/@baseClass,
					$parent := root($element)/*/node()[@baseClass = $baseClass]
				return 
					if ($parent) 
					then cimtool:count-fields($parent, $total-count)
					else $total-count
			else
				$total-count
		"/>
		
		<!-- Convert the integer variable to a sequence and return it -->
		<xsl:sequence select="$result-count"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:inherits-from
		Purpose: Checks if an element inherits from a given baseClass (recursively checks SuperType hierarchy)
		Parameters: 
			$element - The element to check (Root or ComplexType)
			$targetBaseClass - The baseClass to check for in the inheritance hierarchy
		Returns: xs:boolean - true if element inherits from targetBaseClass, false otherwise
	-->
	<xsl:function name="cimtool:inherits-from" as="xs:boolean">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="targetBaseClass" as="xs:string"/>
		
		<xsl:variable name="result" select="
			if ($element/@baseClass = $targetBaseClass) then
				(: Direct match - this element has the target baseClass :)
				true()
			else if ($element/a:SuperType) then
				(: Has a parent - check if parent inherits from target :)
				let $superBaseClass := $element/a:SuperType/@baseClass,
					$parent := (root($element)//a:Root[@baseClass = $superBaseClass]|root($element)//a:ComplexType[@baseClass = $superBaseClass])[1]
				return 
					if ($parent) 
					then cimtool:inherits-from($parent, $targetBaseClass)
					else false()
			else
				(: No match and no parent - doesn't inherit :)
				false()
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:model-reference
		Purpose: Gets the model reference for an element (dataType, baseClass, or baseProperty)
		Parameters: 
			$element - The element to get the model reference from
		Returns: xs:string - The model reference value, or empty string if none exists
		
		Rules:
		- SimpleType: uses @dataType
		- EnumeratedType/CompoundType/ComplexType/Root: uses @baseClass
		- Other elements (attributes/associations): uses @baseProperty (if present)
	-->
	<xsl:function name="cimtool:model-reference" as="xs:string">
		<xsl:param name="element" as="element()"/>
		
		<xsl:variable name="result" select="
			if ($element/self::a:SimpleType) then
				fn:string($element/@dataType)
			else if ($element/self::a:EnumeratedType or 
					 $element/self::a:CompoundType or 
					 $element/self::a:ComplexType or 
					 $element/self::a:Root) then
				fn:string($element/@baseClass)
			else if ($element/@baseProperty) then
				fn:string($element/@baseProperty)
			else
				''
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	
	<!-- 
		Function: cimtool:get-union-dependencies
		Purpose: Gets all concrete subclasses (Root elements) that inherit from an abstract class (ComplexType)
				 These represent the union members in Avro schema.
		Parameters: 
			$abstract-element - The ComplexType element (abstract class)
		Returns: xs:string* - Sequence of baseClass values for all concrete subclasses
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
		Purpose: Gets all distinct @baseClass values from child elements, 
				 including inherited dependencies from SuperType hierarchy.
				 For Instance/Reference to abstract classes (ComplexType), includes union members.
				 Excludes a:SuperType itself and any types in the exclusion map.
		Parameters: 
			$element - The element to examine (CompoundType, Root, etc.)
			$exclusion-map - Map where keys are baseClass values to exclude
		Returns: xs:string* - Sequence of baseClass values (filtered)
	-->
	<xsl:function name="cimtool:get-dependencies" as="xs:string*">
		<xsl:param name="element" as="element()"/>
		<xsl:param name="exclusion-map" as="map(xs:string, xs:boolean)"/>
		
		<xsl:variable name="result" select="
			distinct-values(
				(
					(: Dependencies from non-Instance/Reference/InverseInstance/InverseReference children. These we need to exclude. :)
					$element/*[@baseClass][not(self::a:SuperType)][not(self::a:Instance)][not(self::a:Reference)][not(self::a:InverseInstance)][not(self::a:InverseReference)]/string(@baseClass)[not(map:contains($exclusion-map, .))],
					
					(: Dependencies from Instance/Reference children - handle unions :)
					for $assoc in $element/(a:Instance|a:Reference)[@baseClass]
					return
						let $assoc-baseClass := string($assoc/@baseClass),
							$referenced-element := root($element)/*/node()[@baseClass = $assoc-baseClass]
						return
							if ($referenced-element/self::a:ComplexType) then
								(: Abstract class - get union members (concrete subclasses) :)
								cimtool:get-union-dependencies($referenced-element)[not(map:contains($exclusion-map, .))]
							else if ($referenced-element/self::a:Root) then
								(: Concrete class - use directly :)
								if (not(map:contains($exclusion-map, $assoc-baseClass))) then
									$assoc-baseClass
								else
									()
							else
								(: Referenced element not found or other type - include if not excluded :)
								if (not(map:contains($exclusion-map, $assoc-baseClass))) then
									$assoc-baseClass
								else
									(),
					
					(: If has SuperType, recursively get parent's dependencies :)
					if ($element/a:SuperType) then
						let $supertype_baseClass := $element/a:SuperType/@baseClass,
							$parent := root($element)/*/node()[@baseClass = $supertype_baseClass]
						return
							if ($parent) then
								cimtool:get-dependencies($parent, $exclusion-map)
							else
								()
					else
						()
				)
			)
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>

    <!-- 
        Function: cimtool:build-dependencies-map
        Purpose: Creates a map of type dependencies for the provided elements
        Parameters: 
            $elements - Node-set of elements to process
            $exclusion-map - Map of baseClass values to exclude from dependencies
        Returns: map(xs:string, xs:string*)
                 - Key: baseClass of each element
                 - Value: Sequence of baseClass values from child elements (filtered)
    -->
    <xsl:function name="cimtool:build-dependencies-map" as="map(xs:string, xs:string*)">
        <xsl:param name="elements" as="element()*"/>
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
		Function: cimtool:create-exclusions-map
		Purpose: Creates a map for excluding types based on their @baseClass attribute
		Parameters: 
			$elements - Node-set of elements whose baseClass should be excluded
		Returns: map(xs:string, xs:boolean)
				 - Key: baseClass attribute value
				 - Value: true()
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
		Function: cimtool:is-abstract-with-subclasses
		Purpose: Checks if a baseClass references an abstract class (ComplexType) that has concrete subclasses (Roots)
		Parameters: 
			$baseClass - The baseClass URI to check
		Returns: xs:boolean - true if it's an abstract class with concrete subclasses, false otherwise
	
	<xsl:function name="cimtool:is-abstract-with-subclasses" as="xs:boolean">
		<xsl:param name="baseClass" as="xs:string"/>
		
		<xsl:variable name="is-complex-type" select="exists(root()//a:ComplexType[@baseClass = $baseClass])"/>
		
		<xsl:variable name="result" select="
			if ($is-complex-type) then
				(: Count concrete subclasses :)
				let $concrete-count := count(
					for $root in root()//a:Root
					return
						if (cimtool:inherits-from($root, $baseClass))
						then $root
						else ()
				)
				return $concrete-count > 0
			else
				false()
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
	-->

	<!-- 
		Function: cimtool:topological-sort
		Purpose: Sorts elements in topological order based on their dependencies.
				 Elements with no dependencies come first, then elements that depend only on 
				 already-processed elements, and so on.
				 For circular dependencies, falls back to document order.
		Parameters: 
			$elements - The elements to sort (e.g., //a:CompoundType or //a:Root)
			$deps-map - Dependency map created by cimtool:build-dependencies-map
		Returns: element()* - Sequence of elements in topological (dependency) order
	-->
	<xsl:function name="cimtool:topological-sort" as="element()*">
		<xsl:param name="elements" as="element()*"/>
		<xsl:param name="deps-map" as="map(xs:string, xs:string*)"/>
		
		<xsl:variable name="result" select="cimtool:topological-sort-helper($elements, $deps-map, ())"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>

	<!-- 
		Function: cimtool:topological-sort-helper
		Purpose: Recursive helper for topological sort with accumulated results
		Parameters: 
			$remaining - Elements not yet sorted
			$deps-map - Dependency map
			$sorted - Accumulated sorted elements
		Returns: element()* - Fully sorted sequence
	-->
	<xsl:function name="cimtool:topological-sort-helper" as="element()*">
		<xsl:param name="remaining" as="element()*"/>
		<xsl:param name="deps-map" as="map(xs:string, xs:string*)"/>
		<xsl:param name="sorted" as="element()*"/>
		
		<xsl:choose>
			<xsl:when test="fn:empty($remaining)">
				<!-- Base case: all elements sorted -->
				<xsl:sequence select="$sorted"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- Get baseClasses of already sorted elements -->
				<xsl:variable name="sorted-baseClasses" select="
					for $elem in $sorted 
					return string($elem/@baseClass)
				"/>
				
				<!-- Find elements whose dependencies are all satisfied -->
				<xsl:variable name="ready-elements" select="
					for $elem in $remaining
					return
						let $elem-baseClass := fn:string($elem/@baseClass),
							$elem-deps := $deps-map($elem-baseClass)
						return
							if (every $dep in $elem-deps satisfies $dep = $sorted-baseClasses) then
								$elem
							else
								()
				"/>
				
				<xsl:choose>
					<xsl:when test="fn:exists($ready-elements)">
						<!-- Process ready elements and recurse -->
						<xsl:variable name="new-sorted" select="$sorted, $ready-elements"/>
						<xsl:variable name="new-remaining" select="$remaining except $ready-elements"/>
						<xsl:sequence select="cimtool:topological-sort-helper($new-remaining, $deps-map, $new-sorted)"/>
					</xsl:when>
					<xsl:otherwise>
						<!-- Circular dependency detected - fall back to document order -->
						<xsl:sequence select="$sorted"/>
						<xsl:sequence select="$remaining"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:function>

	<!-- 
		Function: cimtool:get-root-fields
		Purpose: Identifies top-level Root classes that are never referenced by other classes.
				 These are the true "message roots" that should appear at the top level of the schema.
				 Handles both direct references and references through abstract classes (unions).
		Parameters: 
			$all-roots - All a:Root elements in the profile
		Returns: element()* - Sequence of Root elements that are never referenced by any other class
	-->
	<xsl:function name="cimtool:get-root-fields" as="element()*">
		<xsl:param name="all-roots" as="element()*"/>
		
		<!-- Build a set of all baseClass values referenced by Instance/Reference in ANY Root or ComplexType -->
		<xsl:variable name="direct-references" select="
			distinct-values(
				for $element in root($all-roots[1])//(a:Root | a:ComplexType)
				return
					for $child in $element/(a:Instance | a:Reference)
					return string($child/@baseClass)
			)
		"/>
		
		<!-- For each direct reference, find all Root classes that inherit from it -->
		<xsl:variable name="indirect-references" select="
			distinct-values(
				for $ref in $direct-references
				return
					(: Get all Root classes that inherit from this reference :)
					for $root in $all-roots
					return
						if (cimtool:inherits-from($root, $ref))
						then string($root/@baseClass)
						else ()
			)
		"/>
		
		<!-- Combine direct and indirect references -->
		<xsl:variable name="all-references" select="($direct-references, $indirect-references)"/>
		
		<!-- Return roots whose baseClass is NOT in the referenced set -->
		<xsl:variable name="result" select="
			for $root in $all-roots
			return
				if (not(string($root/@baseClass) = $all-references))
				then $root
				else ()
		"/>
		
		<xsl:sequence select="$result"/>
	</xsl:function>
		
	<xsl:template name="generate-fields">
		<xsl:choose>
			<xsl:when test="a:SuperType">
				<xsl:variable name="supertype_name" select="a:SuperType/@name"/>
				<xsl:for-each select="/*/node()[@name = $supertype_name]">
					<xsl:call-template name="generate-fields"/>
				</xsl:for-each>
				<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:apply-templates select="a:Complex|a:Enumerated|a:Compound|a:SimpleEnumerated|a:Simple|a:Domain|a:Instance|a:Reference|a:Choice"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
				
	<xsl:template match="a:Catalog">
		<!-- the top level template -->
		<document>
			<list begin="[" indent="     " delim="," end="]">
			
				<!-- Apache Avro instance data header definition. -->
				<list begin="{{" indent="     " delim="," end="}}">
					<item>"type": "record"</item>
					<item>"name": "Header"</item>
					<item>"namespace": "<xsl:value-of select="$package_prefix"/>"</item>
					<list begin="&quot;fields&quot;: [" indent="     " delim="," end="]">
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "profProfile"</item>
							<item>"type": "string"</item>
							<item>"doc": "URI of the DX-PROF profile this dataset conforms to, e.g. https://ap.cim4.eu/StateVariables/3.0"</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "identifier"</item>
							<item>"type": "string"</item>
							<item>"doc": "Dataset identifier, aligned with dcterms:identifier / dcat:Dataset.@about."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "isVersionOf"</item>
							<item>"type": [ "null", "string" ]</item>
							<item>"doc": "URI of the logical dataset or model this is a version of (dct:isVersionOf)."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "version"</item>
							<item>"type": [ "null", "string" ]</item>
							<item>"doc": "Version label for this dataset instance (aligned with dcat:version). Useful when multiple messages represent different versions of the same time slice."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "startDate"</item>
							<item>"type": "string"</item>
							<item>"doc": "Start of the validity interval for this dataset, aligned with dcat:startDate (ISO-8601). Typically the case time of the power system state."</item>
						</list>
						<list begin="{{" indent="     " delim="," end="}}">
							<item>"name": "schemaRef"</item>
							<item>"type": "string"</item>
							<item>"doc": "Dereferenceable URI or Schema Registry URL for the Avro schema used to encode this dataset."</item>
						</list>					
					</list>
				</list>
			
				<!-- Enumerations can be generated first as they should have no dependencies -->
				<xsl:apply-templates select="a:EnumeratedType"/>
				
				<!-- Step 1: Create exclusion map for EnumeratedTypes ONLY -->
				<xsl:variable name="compound-exclusions" select="cimtool:create-exclusions-map(//a:EnumeratedType)"/>
				
				<!-- Step 2: Create dependency map for CompoundTypes. Note we exclude any dependencies to enumerations since they've been processed. -->
				<xsl:variable name="compound-deps-map" select="cimtool:build-dependencies-map(//a:CompoundType, $compound-exclusions)"/>

				<!-- Step 3: Process CompoundTypes in correct dependency order. -->
				<xsl:variable name="sorted-compound-types" select="cimtool:topological-sort(//a:CompoundType, $compound-deps-map)"/>
				<xsl:apply-templates select="$sorted-compound-types"/>
				
				<!-- Step 4: Create exclusion map for EnumeratedTypes AND CompoundTypes (already processed) -->
				<xsl:variable name="root-exclusions" select="cimtool:create-exclusions-map(//a:EnumeratedType|//a:CompoundType)"/>
				
				<!-- Step 5: Finally, we create dependency map for Root types excluding EnumeratedTypes and CompoundTypes -->
				<!-- Note that we do not include a:ComplexType as they are abstract and Avro does not support inheritance -->
				<!-- and instead the inheritance hierarchy is "flattened" and all attributes and associations are in the  -->
				<!-- concrete (i.e. a:Root) classes.                                                                      -->
				<xsl:variable name="root-deps-map" select="cimtool:build-dependencies-map(//a:Root, $root-exclusions)"/>
				
				<!-- Step 6: Process Root elements in correct dependency order -->
				<xsl:variable name="sorted-root-types" select="cimtool:topological-sort(//a:Root, $root-deps-map)"/>
				<xsl:apply-templates select="$sorted-root-types"/>
				
				<!-- The final step is to create the 'document wrapper' derived from the name of the profile -->
				<list begin="{{" indent="     " delim="," end="}}">
					<xsl:if test="$copyright-single-line and $copyright-single-line != ''">
						<item>"copyright": "<xsl:value-of select="$copyright-single-line" disable-output-escaping="yes"/>"</item>			
					</xsl:if>
					<item>"generator": "Generated by CIMTool https://cimtool.ucaiug.io"</item>
					<list begin="&quot;profile-metadata&quot;: {{" indent="    " delim="," end="}}">
						<item>"metaDoc": "Abstract profile this schema implements (DX-PROF prof:Profile):"</item>
						<item>"profProfile": "<xsl:value-of select="$baseURI"/>"</item>
						<item>"metaDoc": "Underlying standards the profile/schema conforms to (IEC etc.):"</item>
						<item>"metaDoc": "Currently hardcoded until consensus is reached on how/where this will be specified/sourced from in tooling:"</item>
						<list begin="&quot;dctConformsTo&quot;: [" indent="     " delim="," end="]">
							<item>"urn:iso:std:iec:61970-301:ed-7:amd1"</item>
							<item>"urn:iso:std:iec:61970-600-2:ed-1"</item>
						</list>
						<item>"metaDoc": "Where this schema was generated from (OWL/RDFS/SHACL/LinkML, etc.):"</item>
						<list begin="&quot;dctSource&quot;: [" indent="     " delim="," end="]">
							<item>"<xsl:value-of select="concat(substring-before($baseURI, '#'), '/owl')"/>"</item>
						</list>
						<item>"metaDoc": "Identity + version of this Avro schema itself:"</item>
						<item>"metaDoc": "Currently hardcoded until consensus is reached on how/where this will be specified/sourced from in tooling:"</item>
						<item>"schemaId": "https://schema-registry.example.com/subjects/cim-sv-dataset-value/versions/5"</item>
						<item>"metaDoc": "Currently hardcoded to 1.0.0 until consensus is reached on how/where this will be specified/sourced from in tooling:"</item>
						<item>"schemaVersion": "<xsl:value-of select="'1.0.0'"/>"</item>
					</list>
				
					<item>"type": "record"</item>
					<item>"name": "<xsl:value-of select="$envelope"/>"</item>
					<item>"namespace": "<xsl:value-of select="$package_prefix"/>"</item>
					<item>"doc": "<xsl:call-template name="annotate"/>"</item>

						<list begin="&quot;fields&quot;: [" indent="     " delim="," end="]">
							<list begin="{{" indent="     " delim="," end="}}">
								<item>"name": "header"</item>
								<item>"type": "<xsl:value-of select="concat($package_prefix, '.', 'Header')"/>"</item>
								<item>"doc": "The standardized messaging header that must be included with each message compliant with this profile."</item>
							</list>
							
							<!-- Get all Root elements -->
							<xsl:variable name="all-roots" select="//a:Root"/>
							
							<!-- Find true root fields (never referenced) -->
							<xsl:variable name="root-fields" select="cimtool:get-root-fields($all-roots)"/>

							<xsl:for-each select="$root-fields">
								<list begin="{{" indent="     " delim="," end="}}">
									<item>"name": "<xsl:value-of select="@name"/>"</item>
									<xsl:choose>
										<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
											<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
												<item>"type": "array"</item>
												<item>"items": "<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
											</list>
											<xsl:if test="@minOccurs = 0">
												<item>"default": []</item>
											</xsl:if>
										</xsl:when>	
										<xsl:otherwise>
											<item>"type": "<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
										</xsl:otherwise>
									</xsl:choose>
									<item>"doc": "<xsl:call-template name="annotate"/>"</item>
									<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
										<xsl:if test="@minOccurs != 0">
											<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
										</xsl:if>
										<xsl:if test="not(@maxOccurs = 'unbounded')">
											<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
										</xsl:if>
									</xsl:if>	
								</list>
							</xsl:for-each>
						</list>
						
				</list>		
							
			</list>
		</document>
	</xsl:template>
				
	<!-- Note that for AVRO schemas we don't generate Root classes that don't have any attributes. These are considered association references to "external" entities. -->
	<xsl:template match="a:Root">
		<xsl:variable name="fieldCount" select="cimtool:count-fields(.)"/>
		<xsl:if test="$fieldCount > 0">
			<list begin="{{" indent="     " delim="," end="}}">
				<item>"type": "record"</item>
				<item>"name": "<xsl:value-of select="@name"/>"</item>
				<item>"namespace": "<xsl:value-of select="cimtool:package-name(., $package_prefix)"/>"</item>
				<item>"doc": "<xsl:call-template name="annotate"/>"</item>
				<item>"modelReference": "<xsl:value-of select="cimtool:model-reference(.)"/>"</item>
				<list begin="&quot;fields&quot;: [" indent="     " delim="," end="]">
					<xsl:call-template name="generate-fields"/>
				</list>
			</list>
		</xsl:if>
    </xsl:template>
    
    <xsl:template match="a:CompoundType">
		<list begin="{{" indent="     " delim="," end="}}">
			<item>"type": "record"</item>
			<item>"name": "<xsl:value-of select="@name"/>"</item>
			<item>"namespace": "<xsl:value-of select="cimtool:package-name(., $package_prefix)"/>"</item>
			<item>"doc": "<xsl:call-template name="annotate"/>"</item>
			<item>"modelReference": "<xsl:value-of select="cimtool:model-reference(.)"/>"</item>
			<list begin="&quot;fields&quot;: [" indent="     " delim="," end="]">
				<xsl:call-template name="generate-fields"/>
			</list>
		</list>
    </xsl:template>
    	
    <xsl:template match="a:EnumeratedType">
		<list begin="{{" indent="     " delim="," end="}}">
			<item>"type": "enum"</item>
			<item>"name": "<xsl:value-of select="@name"/>"</item>
			<item>"namespace": "<xsl:value-of select="cimtool:package-name(., $package_prefix)"/>"</item>
			<item>"doc": "<xsl:call-template name="annotate"/>"</item>
			<item>"modelReference": "<xsl:value-of select="cimtool:model-reference(.)"/>"</item>
			<list begin="&quot;symbols&quot;: [" indent="     " delim="," end="]">
				<xsl:for-each select="a:EnumeratedValue">
					<item>"<xsl:value-of select="@name"/>"</item>
				</xsl:for-each>
			</list>
		</list>
	</xsl:template>
	
	<xsl:template match="a:Simple|a:Domain">
		<list begin="{{" indent="     " delim="," end="}}">
			<xsl:variable name="type" select="cimtool:get-avro-type(@xstype)"/>
			<item>"name": "<xsl:value-of select="@name"/>"</item>
			<xsl:choose>
				<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
					<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
						<item>"type": "array"</item>
						<item>"items": <xsl:value-of select="$type"/></item>
					</list>
					<xsl:if test="@minOccurs = 0">
						<item>"default": []</item>
					</xsl:if>
				</xsl:when>	
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="@minOccurs = 0">
							<item>"type": [ "null", <xsl:value-of select="$type"/> ]</item>
							<item>"default": null</item>
						</xsl:when>
						<xsl:otherwise>
							<item>"type": <xsl:value-of select="$type"/></item>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			<item>"doc": "<xsl:call-template name="annotate"/>"</item>
			<item>"modelReference": "<xsl:value-of select="cimtool:model-reference(.)"/>"</item>
			<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
				<xsl:if test="@minOccurs != 0">
					<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
				</xsl:if>
				<xsl:if test="not(@maxOccurs = 'unbounded')">
					<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
				</xsl:if>
			</xsl:if>	
		</list>
	</xsl:template>
	
	<xsl:template match="a:Compound|a:Enumerated">
		<list begin="{{" indent="     " delim="," end="}}">
			<item>"name": "<xsl:value-of select="@name"/>"</item>
			<xsl:choose>
				<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
					<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
						<item>"type": "array"</item>
						<item>"items": "<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
					</list>
					<xsl:if test="@minOccurs = 0">
						<item>"default": []</item>
					</xsl:if>
				</xsl:when>	
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="@minOccurs = 0">
							<list begin="&quot;type&quot;: [" indent="     " delim="," end="]">
								<item>"null"</item>
								<item>"<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
							</list>
							<item>"default": null</item>
						</xsl:when>
						<xsl:otherwise>
							<item>"type": "<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
			<item>"doc": "<xsl:call-template name="annotate"/>"</item>
			<item>"modelReference": "<xsl:value-of select="cimtool:model-reference(.)"/>"</item>
			<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
				<xsl:if test="@minOccurs != 0">
					<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
				</xsl:if>
				<xsl:if test="not(@maxOccurs = 'unbounded')">
					<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
				</xsl:if>
			</xsl:if>	
		</list>
	</xsl:template>
	
	<xsl:template match="a:Instance|a:Reference">
		<xsl:variable name="baseClass" select="@baseClass"/>
		<xsl:variable name="theClass" select="//a:Root[@baseClass = $baseClass]|//a:ComplexType[@baseClass = $baseClass]"/>
		<xsl:variable name="fullyQualifiedClass" select="cimtool:fully-qualified-class($theClass, $package_prefix)"/>
		<xsl:variable name="fieldCount" select="cimtool:count-fields($theClass)"/>
		
		<!-- Check if this references an abstract class (ComplexType) that has concrete subclasses -->
		<xsl:variable name="isAbstractWithSubclasses">
			<xsl:choose>
				<xsl:when test="//a:ComplexType[@baseClass = $baseClass]">
					<!-- This references a ComplexType - check if there are Root subclasses -->
					<xsl:variable name="concreteSubclasses">
						<xsl:for-each select="//a:Root">
							<xsl:if test="cimtool:inherits-from(., $baseClass)">
								<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>
							</xsl:if>
						</xsl:for-each>
					</xsl:variable>
					<xsl:choose>
						<xsl:when test="string-length($concreteSubclasses) > 0">
							<xsl:text>true</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>false</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>false</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- 
		<xsl:message select="'===================================='"/>
		<xsl:message select="concat('#1 Instance/Reference:  ', @name)"/>
		<xsl:message select="concat('#2 isAbstractWithSubclasses:  ', $isAbstractWithSubclasses)" />
		<xsl:message select="concat('#3 cimtool:is-abstract-with-subclasses:  ', string(cimtool:is-abstract-with-subclasses($baseClass)))" /> 
		-->
		<list begin="{{" indent="     " delim="," end="}}">	
			<xsl:choose>
				<xsl:when test="$fieldCount > 0">
					<item>"name": "<xsl:value-of select="@name"/>"</item>
					<xsl:choose>
						<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
							<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
								<item>"type": "array"</item>
								<xsl:choose>
									<!-- Union case: array of union types -->
									<xsl:when test="$isAbstractWithSubclasses = 'true'">
										<list begin="&quot;items&quot;: [" indent="     " delim="," end="]">
											<xsl:for-each select="//a:Root">
												<xsl:if test="cimtool:inherits-from(., $baseClass)">
													<item>"<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
												</xsl:if>
											</xsl:for-each>
										</list>
									</xsl:when>
									<!-- Normal case: array of single type -->
									<xsl:otherwise>
										<item>"items": "<xsl:value-of select="$fullyQualifiedClass"/>"</item>
									</xsl:otherwise>
								</xsl:choose>
							</list>
							<xsl:if test="@minOccurs = 0">
								<item>"default": []</item>
							</xsl:if>
						</xsl:when>	
						<xsl:otherwise>
							<xsl:choose>
								<xsl:when test="@minOccurs = 0">
									<xsl:choose>
										<!-- Union case: nullable union of types -->
										<xsl:when test="$isAbstractWithSubclasses = 'true'">
											<list begin="&quot;type&quot;: [" indent="     " delim="," end="]">
												<xsl:for-each select="//a:Root">
													<xsl:if test="cimtool:inherits-from(., $baseClass)">
														<item>"<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
													</xsl:if>
												</xsl:for-each>
											</list>
										</xsl:when>
										<!-- Normal case: nullable single type -->
										<xsl:otherwise>
											<list begin="&quot;type&quot;: [" indent="     " delim="," end="]">
												<item>"null", "<xsl:value-of select="$fullyQualifiedClass"/>"</item>
											</list>
										</xsl:otherwise>
									</xsl:choose>
									<item>"default": null</item>
								</xsl:when>
								<xsl:otherwise>
									<xsl:choose>
										<!-- Union case: required union of types -->
										<xsl:when test="$isAbstractWithSubclasses = 'true'">
											<list begin="&quot;type&quot;: [" indent="     " delim="," end="]">
												<xsl:for-each select="//a:Root">
													<xsl:if test="cimtool:inherits-from(., $baseClass)">
														<item>"<xsl:value-of select="cimtool:fully-qualified-class(., $package_prefix)"/>"</item>
													</xsl:if>
												</xsl:for-each>
											</list>
										</xsl:when>
										<!-- Normal case: required single type -->
										<xsl:otherwise>
											<item>"type": "<xsl:value-of select="$fullyQualifiedClass"/>"</item>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
					<item>"doc": "<xsl:call-template name="annotate"/>"</item>
					<item>"modelReference": "<xsl:value-of select="cimtool:model-reference(.)"/>"</item>
					<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
						<xsl:if test="@minOccurs != 0">
							<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
						</xsl:if>
						<xsl:if test="not(@maxOccurs = 'unbounded')">
							<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
						</xsl:if>
					</xsl:if>
				</xsl:when>
				<xsl:otherwise>
					<item>"name": "<xsl:value-of select="@name"/>"</item>
					<xsl:choose>
						<xsl:when test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
							<list begin="&quot;type&quot;: {{" indent="    " delim="," end="}}">
								<item>"type": "array"</item>
								<item>"items": "string"</item>
							</list>
							<xsl:if test="@minOccurs = 0">
								<item>"default": []</item>
							</xsl:if>
						</xsl:when>	
						<xsl:otherwise>
							<xsl:choose>
								<xsl:when test="@minOccurs = 0">
									<item>"type": [ "null", "string" ]</item>
									<item>"default": null</item>
								</xsl:when>
								<xsl:otherwise>
									<item>"type": "string"</item>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
					<item>"doc": "<xsl:call-template name="annotate"/> Note that the value of this field is the identifier (e.g. mRID) used to reference the <xsl:value-of select="@type"/> external to this profile."</item>
					<item>"modelReference": "<xsl:value-of select="cimtool:model-reference(.)"/>"</item>
					<xsl:if test="@maxOccurs = 'unbounded' or @maxOccurs &gt; 1">
						<xsl:if test="@minOccurs != 0">
							<item>"minCardDoc": "[min cardinality = <xsl:value-of select="@minOccurs"/>] Application level validation will be required to ensure the array contains at least <xsl:value-of select="@minOccurs"/> <xsl:choose><xsl:when test="@minOccurs = 1"> item.</xsl:when><xsl:otherwise> items.</xsl:otherwise></xsl:choose>"</item>
						</xsl:if>
						<xsl:if test="not(@maxOccurs = 'unbounded')">
							<item>"maxCardDoc": "[max cardinality = <xsl:value-of select="@maxOccurs"/>] Application level validation will be required to ensure the array contains at most <xsl:value-of select="@maxOccurs"/> items."</item>
						</xsl:if>
					</xsl:if>
				</xsl:otherwise>
			</xsl:choose>
		</list>
	</xsl:template>
	
	<xsl:template name="annotate">
		<xsl:apply-templates mode="annotate"/>
	</xsl:template>
	
	<xsl:template match="a:Comment|a:Note" mode="annotate">
		<!-- Decode entities and escape properly for JSON -->
		<xsl:value-of select="cimtool:json-escape(cimtool:unescape-numeric(string(.)))"/>
	</xsl:template>
	
	<xsl:template match="node()" mode="annotate">
		<!-- dont pass any defaults in annotate mode -->
	</xsl:template>
	
	<xsl:template match="node()">
	</xsl:template>
	
</xsl:stylesheet>
