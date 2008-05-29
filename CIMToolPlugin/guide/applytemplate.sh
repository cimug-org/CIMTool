#! /bin/sh
set -e -x
script=${1:-applytemplate.xslt}
for t in browse message generate
do
	for f in $t*.svg index.svg
	do
		mv $f $f~
		xml tr $script < $f~ > $f
	done
done
