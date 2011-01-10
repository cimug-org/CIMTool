#! /bin/sh
COMMAND=${1:-package}
VERSION=${2:-$(date --iso)}

package() {

  cd Assembly || exit 1

  for d in linux.* macosx.* win32.*
  do
    if [ -d $d ]
    then
      p=CIMTool-$VERSION-$d.zip
      if [ -f $d/$p ]
      then
	rm $d/$p
      fi
      ( cd $d && zip -q -r $p CIMTool )
      mv $d/$p ../
      echo $p
    fi
  done
}



upload() {
  for p in CIMTool-$VERSION-*.zip
  do
    s3 cp $p files.cimtool.org
  done
}

$COMMAND

