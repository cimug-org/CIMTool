#! /bin/sh
#
#	create the PNG icons for distribution from the SVG artwork
#

die() {
	echo $1
	exit 1
}

convert() {
	inkscape  --export-area-canvas  --file=$1 --export-png=$2.png --export-height=16 --export-width=16
	inkscape  --export-area-canvas  --file=$1 --export-png=$2-32.png --export-height=32 --export-width=32
#	inkscape  --export-area-canvas  --file=$1 --export-png=$2-64.png --export-height=64 --export-width=64
}

convertdir() {
	[ -d $1 ] || die "must execute this in the project directory" 
	[ -d $2 ] || mkdir -p $2

	for svg in $1/*.svg
	do
		convert $svg $2/$(basename $svg .svg)
	done
}

convertdir graphics icons
# cp icons/*.png src/au/com/langdale/ui/icons/
