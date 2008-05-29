#! /bin/bash
# Release script. 
# 
# Assumes builds with correctly formatted names are available in the builds directory.
# Assumes these builds corresponds to svn HEAD for tagging purposes.
# The version part of the build name is the iso date by default or $1 if supplied.
#
set -e
#set -x

#really="echo"
builds=./builds
version=${1:-$(date --iso)}

$really svn cp svn://cimtool.org/branches/eclipse svn://cimtool.org/releases/$version -m"Release Tag"
$really svn cp svn://shag/local/CIMToolPlugin svn://shag/local/Releases/CIMToolPlugin/$version -m"Release Tag"
$really svn cp svn://shag/local/CIMToolTest svn://shag/local/Releases/CIMToolTest/$version -m"Release Tag"

cat < /dev/null > $builds/Release-Notes-$version.txt

for prefix in "CIMTool-Plugin" "CIMTool-Eclipse"
do
	if [ -e $builds/$prefix-$version.zip ]
	then
    		(cd builds && md5sum $prefix-$version.zip > $prefix-$version.md5)

    		$really scp $builds/$prefix-$version.md5 \
                            cimtool@cimtool.org:/var/trac/cimtool/htdocs/static/

		read checksum name < $builds/$prefix-$version.md5

		sed 	-e "s/Bare-VERSION/$version/g" \
			-e "s/$prefix-VERSION/$prefix-$version/g" \
			-e "s/$prefix-CHECKSUM/$checksum/g" \
			< ANNOUNCE-$prefix.txt \
			>> $builds/Release-Notes-$version.txt
    fi
done

cat CHANGES.txt HISTORY.txt README.txt >> $builds/Release-Notes-$version.txt

echo "Don't forget to update http://cimtool.org/cimtool/wiki/Download" 
echo "with contents of $builds/Release-Notes-$version.txt"
echo "And upload the zip files to Amazon S3 ... "