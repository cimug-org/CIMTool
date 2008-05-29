#! /bin/sh
set -e -x
for t in browse message generate complex refine
do
	files=$t*.svg
	links=(index $(for f in $files; do basename $f .svg; done) index)

	((i=1))
	for f in $files
	do
		xml tr svg2html.xslt -s back=${links[i-1]}.html  -s next=${links[i+1]}.html -s finish=index.html < $f > ${links[i]}.html
		((i+=1))
	done
done
xml tr svg2html.xslt < index.svg > index.html