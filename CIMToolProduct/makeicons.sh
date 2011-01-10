#! /bin/sh
#
#	create the PNG icons for distribution from the SVG artwork
#

scaleit() {
	# inkscape  --export-area-page  --file=$1.svg --export-png=$2-$3.png --export-height=$3 --export-width=$3
	convert $1.svg -depth 8 -resize $3x$3 $2-$3.png
	convert $2-$3.png $2-$3.bmp
	convert $2-$3.png +matte -colors 256 $2-$3-8.bmp
}

scaleit icon icon 16
scaleit icon icon 32
scaleit icon icon 48
scaleit icon icon 64
convert icon-32.png icon.xpm
