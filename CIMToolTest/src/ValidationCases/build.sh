#! /bin/sh
for ttl in *.ttl
do
	xml=$(basename $ttl .ttl).xml
	echo $xml
	sh run.sh au.com.langdale.cimtoole.Convert $ttl $xml "TTL" "RDF/XML"
done

