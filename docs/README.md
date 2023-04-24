# CIMTool Docs
This directory stores the CIMTool documentation site, which is deployed [here](https://cimtool.ucaiug.io/).

## Development
The CIMTool documentation site leverages [Material for MkDocs](https://squidfunk.github.io/mkdocs-material). If you need to do work on the documentation site, clone this repo then install Material for MkDocs with either Python `pip install mkdocs-material` or Docker `docker pull squidfunk/mkdocs-material`. 

Once installed, the basic commands are:

* `mkdocs serve` - Start the live-reloading docs server.
* `mkdocs build` - Build the documentation site.
* `mkdocs -h` - Print help message and exit.

With docker (`mkdocs serve` is default command) so you can just do the following to serve the site if you need to work on it:

    docker run --rm -it -v ${PWD}:/docs squidfunk/mkdocs-material new .

A GitHub Action is used to automatically deploy to GitHub Pages when you commit to master branch.
