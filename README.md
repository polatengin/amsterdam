# Run Jenkins on Azure Container Instances

![Run Jenkins on Azure Container Instances](./assets/header.png "Run Jenkins on Azure Container Instances")

With this guideline, you can deploy and run [Jenkins](https://jenkins.io/) on your local machine as well as on [Azure Container Instances](https://azure.microsoft.com/en-us/services/container-instances/).

* [Guideline to Install Jenkins on an existing Ubuntu machine](#installing-jenkins-on-an-existing-ubuntu-machine)

* [Guideline to build custom Jenkins image and run it on your machine](#guideline-to-build-custom-jenkins-image-and-run-it-on-your-machine)

* [Guideline to Install Jenkins on Azure Container Instances](#installing-jenkins-on-azure-container-instances)

## Jenkins

[Jenkins](https://jenkins.io/) is one of the most popular open-source _DevOps_ tools and it has been widely adopted by many development projects as a leading _CI engine_.

_Jenkins Documentation_ can be found on the [Jenkins User Documentation](https://jenkins.io/doc/) page.

## Fun Facts about Jenkins

* Jenkins is a cross-platform Continuous Integration tool that builds and test software projects continuously.

* Jenkins is developed in Java programming languages that provide real-time testing and reporting.

* Jenkins provides hundreds of plugins to support building, deploying and automating any project.

* Jenkins has both a GUI interface and console commands.

* Jenkins is an open-source CI tool completely compiled and written in Java that originated as a subsidiary of Oracle created by Sun Microsystems.

## Guideline to Install Jenkins on an existing Ubuntu machine

* Make sure that system has [Java JDK](https://openjdk.java.net/install/) on it

```bash
sudo apt update
sudo apt install openjdk-8-jdk
```

* Add Jenkins Repository

```bash
wget -q -O - https://pkg.jenkins.io/debian/jenkins.io.key | sudo apt-key add –
sudo sh -c 'echo deb http://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
sudo apt update
```

* Install Jenkins

```bash
sudo apt install jenkins
```

Jenkins will run on [http://localhost:8080](http://localhost:8080)

![Jenkins is getting ready to work](./assets/ss-0.png)

After a few seconds, Jenkins will be redirected to _Getting Started_ page and wait for the user to enter the initial administrator password.

![Jenkins getting started](./assets/ss-1.png)

After unlocking Jenkins, it requires selecting a _plugin set_ to install. Jenkins can be installed with a pre-defined set of plugins, or, you can select which plugins you want to install with Jenkins.

Installation usually takes a couple of minutes.

![Jenkins installation](./assets/ss-2.gif)

After installation is done, Jenkins will redirect to _Main Page_

Boom! 💣 It's done!

You can use Jenkins, now 🎉

![Jenkins Main Page](./assets/ss-3.png)

## Problem

Installing Jenkins requires so many manual steps, it's not so easy to automate to ready a Jenkins machine.

Also, you need to prepare a machine to install Jenkins. It's your responsibility to make this machine up and running all the time.

So, you need to take care of backup and restore plans, you need to plan for disaster scenarios, etc.

## Guideline to build custom Jenkins image and run it on your machine

The following guideline will help you to;

* Build a custom _Docker Image_ for Jenkins which has plugins you want to use

It the end, you'll have a Jenkins instance running on you local machine.

Let's start;

* Create a Dockerfile with the following content

```dockerfile
FROM jenkins/jenkins:2.230

ARG JENKINS_USERNAME=demouser
ARG JENKINS_PASSWORD=demo@pass123

ENV JENKINS_USERNAME $JENKINS_USERNAME
ENV JENKINS_PASSWORD $JENKINS_PASSWORD
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false

RUN echo "Starting installation of Jenkins Plugins" \
    && /usr/local/bin/install-plugins.sh \
                              "git" \
                              "azure-commons" \
                              "azure-acs" \
                              "azure-app-service" \
                              "azure-cli" \
    && echo "Done"

COPY ./default-user.groovy /usr/share/jenkins/ref/init.groovy.d/
```

As you can see, we're using the latest version of Jenkins (at the time of this writing) which is version 2.230

We have 2 `ARG` commands to accept some variables during `docker build`

Also, we have 2 `ENV` commands to use the `ARG` variables to create environment variables. Those environment variables will set the administrator user name and password for Jenkins.

There is a list of plugins that will be installed with the Jenkins. You can add or remove plugins by modifying the plugins list.

You can find a list of all plugins on [Jenkins Plugin Index](https://plugins.jenkins.io/)

Get the _ID_ of a plugin (for example, `azure-acs` for _Azure Container Service_) and add it the list of plugins in the [Dockerfile](./src/Dockerfile)

![Jenkins Azure Container Service Plugin](./assets/ss-4.png)

The problem is, we still have to create a _Jenkins user_ and set it as the _default_ Jenkins user.

_Jenkins_, _during installation_, can (and _will_) load [Groovy](http://groovy-lang.org/) files if they're in the `/usr/share/jenkins/ref/` folder.

Last line of the [Dockerfile](./src/Dockerfile) copies [default-user.groovy](./src/default-user.groovy) file into the correct folder in the Docker Image.

So, Jenkins will not need the user to create _Jenkins User_ during installation.

At this point, we step closer to the fully automated _Jenkins_ installation.

Let's build the _Docker Image_ first;

```bash
docker build -t jenkins:v1 .
```

![Docker Build Jenkins Image](./assets/ss-5.png)

After the successful build of the _Jenkins Docker Image_, we can run it on our machines to test it properly;

```bash
docker run -d -it -p 8080:8080 jenkins:v1
```

![Docker Run Jenkins Image](./assets/ss-6.png)

Let's test it on the browser, open [https://localhost:8080](https://localhost:8080) on your favorite browser (mine is, [Chromium-based Edge](https://www.microsoft.com/en-us/edge))

![Jenkins Main Page](./assets/ss-7.png)

You should see _Jenkins Main Page_, but this time from _Docker Container_.

## Going to Azure

Now we have a running Jenkins installation on our machines, thanks to custom Jenkins image and Docker. But it's not highly available, it's not availabe at all, for most of the cases.

Having a virtual machine somewhere (probably on cloud) solve availability but not high-availability problems, we need to have at least 2 identical virtual machines which has Jenkins on them.

Also we need to utilize Monitoring solution and Load Balancer type mechanism to make Jenkins installation _highly available_.

_Azure Container Instances_ _may_ solve availability and high availability problems at once.

## Guideline to Install Jenkins on Azure Container Instances

After creating a custom _Jenkins_ Docker image on your machine (you can follow guideline on [Guideline to build custom Jenkins image and run it on your machine](#guideline-to-build-custom-jenkins-image-and-run-it-on-your-machine)) it's time to push Docker image to Azure Container Registry and run it on Azure Container Instances.

So, steps are;

* Push custom Jenkins Docker Image to Azure Container Registry

* Create an Azure Container Instance based on the custom Jenkins Docker Image from the Azure Container Registry

It the end, you'll have a Jenkins instance running on Azure Container Instances.

* Create the Azure Resource Group first;

```bash
az group create --location westeurope --name amsterdamrg
```

* We can create Azure Container Registry in the Resource Group now;

```bash
az acr create --resource-group amsterdamrg --name amsterdamacr --sku basic --admin-enabled true
```

* We need to get credentials to push a Docker image to Azure Container Registry

```bash
USERNAME=`az acr credential show --resource-group amsterdamrg --name amsterdamacr --query username -o tsv`
PASSWORD=`az acr credential show --resource-group amsterdamrg --name amsterdamacr --query passwords[0].value -o tsv`
```

* [Optional] We can use already built image from [Guideline to build custom Jenkins image and run it on your machine](#guideline-to-build-custom-jenkins-image-and-run-it-on-your-machine) _or_ we can build Jenkins image here, with Azure Container Registry Build Docker Image service;

```bash
az acr build --image jenkins:latest --registry amsterdamacr --file ./Dockerfile .
```

* Last (_but not least_) we need to create an _Azure Container Instance_ from _Jenkins_ image in the _Azure Container Registry_

```bash
FQDN=`az container create \
  --name "amsterdamjenkins" \
  --resource-group amsterdamrg \
  --image "amsterdamacr.azurecr.io/jenkins:latest" \
  --registry-login-server "$USERNAME.azurecr.io" \
  --registry-username $USERNAME \
  --registry-password $PASSWORD \
  --dns-name-label "amsterdamjenkins" \
  --port 8080 \
  --query ipAddress.fqdn \
  --output tsv`
```

* Now we can print url of the newly created _Jenkins_ instance on _Azure Container Instances_ to _Terminal_

```bash
echo "You can click http://$FQDN:8080 to launch Jenkins interface on your browser"
```

## References

* [Jenkins](https://jenkins.io/)

* [Jenkins User Documentation](https://jenkins.io/doc/)

* [Azure Container Instances](https://azure.microsoft.com/en-us/services/container-instances/)

* [OpenJDK](https://openjdk.java.net/install/)

* [Debian Jenkins Packages](https://pkg.jenkins.io/debian/)

* [Jenkins Cheat Sheet](https://www.edureka.co/blog/cheatsheets/jenkins-cheat-sheet/)

* [OpenShift Pipelines with Jenkins Blue Ocean](https://www.openshift.com/blog/openshift-pipelines-jenkins-blue-ocean)
