~/share/scala-2.7.7.RC1/bin/scaladoc -d api  -cp $(echo build/* | sed -e 's/ /:/g') src/au/com/langdale/cimcheck/*.scala
