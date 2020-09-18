# CIMTool Docker Development Environment

# Preparation
This only works on Linux because it relies on the redirection of X-Windows from the docker container
to the host system, i.e. __this will *not* work on Windows__.

This assumes that you locally clone your GitHub fork of the [CIMTool repo](https://github.com/CIMug-org/CIMTool)
and check out the release-1.10.0.RC2 branch. All commands below are run from the root directory ```CIMTool```.

The workflow is:
1. build the Docker container once
  (maybe eventually this container can be pushed to hub.docker.com, but for now you need to build it yourself)
2. run the Docker container to use the Eclipse IDE for plugin development referencing your local CIMTool repo clone
3. use git commands on your host system to add files to be checked in 
4. submit your commits from the host system
5. repeat from step 2

# Build Docker Image
The magic incantation to build a docker image of the development environment
is run from the root directory of the CIMTool clone.
This will take a while to download requested binaries and install them.
The choice of namespace is up to you, but ```cimtool``` is assumed below.

    CIMTool$ docker build --file docker/Dockerfile.indigo --tag cimtool/eclipse-docker:indigo .

This should produce a Docker image:

    CIMTool$ docker images
    REPOSITORY               TAG     IMAGE ID       SIZE,  CREATED AT
    cimtool/eclipse-docker   indigo  95918943988b   711MB, 2020-09-18 12:22:32 +0200 CEST
    ...

# Run Eclipse in Docker
The docker/eclipse_indigo file is an example executable script to start the docker container.
This links the X-Windows display to the host and mounts the current director as ```/workspace```

    CIMTool$ docker/eclipse_indigo

The first time it is run it makes a container called ```cimtool_indigo```
and subsequent runs just restarts the container.
Select the location of your workspace. The default of ```CIMTool/docker/indigo/workspace```
means that the workspace will be persisted through docker container restarts.

## Install ScalaIDE Plugin

Follow the instructions to [install the ScalaIDE 3.0.0 Plugin](https://github.com/CIMug-org/CIMTool/blob/gh-pages/dev-env-setup.md#install-scalaide-300-plugin),
except the download and unzip has been done, so start at step 2 and then in step 4 specify
the local directory as ```/opt/site```.

## Import of the CIMTool Project

From the ```File``` menu of Eclipse, choose the ```Import``` item.
In the Import dialog, choose ```General/Existing Projects into Workspace```.
In the ```Import Projects``` tab, ```Browse``` to, or enter ```/workspace``` as the ```Select root directory``` contents.
Click ```Finish```.

After rebuilding the project, there should only be warnings.

# Go!

You can now develop in Eclipse Indigo
and [deploy the updated version](https://github.com/CIMug-org/CIMTool/blob/gh-pages/cim-tool-deploy-instructions.md).
Have fun.