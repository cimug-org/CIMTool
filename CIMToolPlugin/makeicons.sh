die() {
	echo $1
	exit 1
}

convert() {
	inkscape  --export-area-canvas  --file=$1 --export-png=$2 --export-height=16 --export-width=16
}

convertdir() {
	[ -d $1 ] || die "must execute this in the project directory" 
	[ -d $2 ] || mkdir -p $2

	for svg in $1/*.svg
	do
		convert $svg $2/$(basename $svg .svg).png
	done
}

convertdir graphics icons
cp icons/*.png src/au/com/langdale/ui/icons/
