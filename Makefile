###############################################################################
#
#  Copyright (c) 2019. Ontario Institute for Cancer Research
#  
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU Affero General Public License as
#  published by the Free Software Foundation, either version 3 of the
#  License, or (at your option) any later version.
#  
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU Affero General Public License for more details.
#  
#  You should have received a copy of the GNU Affero General Public License
#  along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
###############################################################################

#####################################################################
#          Project Configuration
#####################################################################

# Project Info
SOURCE_COMMIT := $$(git show | grep commit | head -1 | tr -s ' ' | cut -d ' ' -f 2 | cut -c 1-8)
DOCKER_PROJ := $$(sed -n -e 's/.*<name>\(.*\)<\/name>.*/\1/p' ./pom.xml)
ROOT_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))

# Required System files
DOCKER_EXE := /usr/bin/docker
DOCKER_COMPOSE_EXE := /usr/bin/docker-compose

# Required Project files
DOCKERFILE_SERVER := Dockerfile
MVNW_EXE := $(ROOT_DIR)/mvnw
POM_XML_FILE := $(ROOT_DIR)/pom.xml
DOCKER_COMPOSE_FILE := $(ROOT_DIR)/docker-compose.yml

# Commands
MVN_COMMAND := $(MVNW_EXE) -f $(POM_XML_FILE)
DOCKER_COMPOSE_COMMAND := $(DOCKER_COMPOSE_EXE) -f $(DOCKER_COMPOSE_FILE) -p $(DOCKER_PROJ)

# Version Info
DOCKER_EXE_VERSION := $$($(DOCKER_EXE) version --format '{{.Server.Version}}')
DOCKER_COMPOSE_EXE_VERSION := $$($(DOCKER_COMPOSE_EXE) version --short)
MVNW_EXE_VERSION := $$($(MVNW_EXE) -version | head -1 | tr -s ' ' | cut -d ' ' -f 3)
JAVA_VERSION := $$($(MVNW_EXE) -version | grep 'Java version' | sed 's/,//g' | tr -s ' ' | cut -d ' ' -f 3)

#####################################################################
#          Internal
#####################################################################

# STDOUT Formatting
RED := $$(echo  "\033[0;31m")
YELLOW := $$(echo "\033[0;33m")
END := $$(echo  "\033[0m")
ERROR_HEADER :=  [ERROR]:
INFO_HEADER := "**************** "
DONE_MESSAGE := $(YELLOW)$(INFO_HEADER) "- done\n" $(END)

.PHONY: 
.SILENT: docker-server-ps docker-server-logs

# Internal Targets
$(DOCKER_EXE):
	$(error $(ERROR_HEADER) The docker executable "$(DOCKER_EXE)" does not exist)

$(DOCKER_COMPOSE_EXE):
	$(error $(ERROR_HEADER) The docker-compose executable "$(DOCKER_COMPOSE_EXE)" does not exist)

$(POM_XML_FILE):
	$(error $(ERROR_HEADER) The could not find the pom.xml file at $(POM_XML_FILE) )

$(MVNW_EXE):
	$(error $(ERROR_HEADER) The mvnw executable "$(MVNW_EXE)" does not exist)

_check_docker_software_exists: $(DOCKER_EXE) $(DOCKER_COMPOSE_EXE)

_check_mvn_software_exists: $(POM_XML_FILE) $(MVNW_EXE)
	@chmod +x $(MVNW_EXE)

#####################################################################
#          User Targets
#####################################################################

help:
	@echo
	@echo "**************************************************************"
	@echo "                  Help"
	@echo "**************************************************************"
	@echo "To dry-execute a target run: make -n <target> "
	@echo
	@echo "Available Targets: "
	@grep '^[A-Za-z][A-Za-z0-9_-]\+:.*' ./Makefile | sed 's/:.*//' | sed 's/^/\t/'
	@echo

info:
	@echo
	@echo "**************************************************************"
	@echo "                  Info"
	@echo "**************************************************************"
	@echo "               ROOT_DIR:  $(ROOT_DIR)"
	@echo "              JAVA_HOME:  ${JAVA_HOME}"
	@echo "            DOCKER_PROJ:  $(DOCKER_PROJ)"
	@echo "          SOURCE_COMMIT:  $(SOURCE_COMMIT)"
	@echo "           JAVA_VERSION:  $(JAVA_VERSION)"
	@echo "           MVNW_VERSION:  $(MVNW_EXE_VERSION)"
	@echo "         DOCKER_VERSION:  $(DOCKER_EXE_VERSION)"
	@echo "     DOCKER_COMPOSE_EXE:  $(DOCKER_COMPOSE_EXE)"
	@echo "               MVNW_EXE:  $(MVNW_EXE)"
	@echo "             DOCKER_EXE:  $(DOCKER_EXE)"
	@echo " DOCKER_COMPOSE_VERSION:  $(DOCKER_COMPOSE_EXE_VERSION)"
	@echo "      DOCKERFILE_SERVER:  $(DOCKERFILE_SERVER)"
	@echo "    DOCKER_COMPOSE_FILE:  $(DOCKER_COMPOSE_FILE)"
	@echo

# Local developement related targets
clean: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning local project" $(END)
	@$(MVNW_EXE) clean
	@echo $(DONE_MESSAGE)

format: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Formatting local project" $(END)
	@$(MVNW_EXE) fmt:format
	@echo $(DONE_MESSAGE)

proto: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Compiling proto files only" $(END)
	@$(MVNW_EXE) protobuf:compile protobuf:compile-custom
	@echo $(DONE_MESSAGE)

compile: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning and compiling everything" $(END)
	@$(MVNW_EXE) clean compile
	@echo $(DONE_MESSAGE)

verify: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Cleaning and verifying" $(END)
	@$(MVNW_EXE) clean verify
	@echo $(DONE_MESSAGE)

#######################################
dev-start: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Starting DEV mode with ego and admin services"$(END)
	@$(DOCKER_COMPOSE_COMMAND) up --no-deps -d ego-api ego-postgres admin
	@echo $(DONE_MESSAGE)

dev-stop: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Stopping DEV mode ego and admin services"$(END)
	@$(DOCKER_COMPOSE_COMMAND) kill ego-api ego-postgres admin
	@$(DOCKER_COMPOSE_COMMAND) rm -f ego-api ego-postgres admin
	@echo $(DONE_MESSAGE)

dev-ps: demo-ps
dev-logs: demo-logs

fresh-ego: _check_docker_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Restarting a fresh empty instance of the ego service"$(END)
	@$(DOCKER_COMPOSE_COMMAND) kill ego-postgres ego-api
	@$(DOCKER_COMPOSE_COMMAND) rm -f ego-postgres ego-api
	@$(DOCKER_COMPOSE_COMMAND) up --no-deps -d ego-postgres ego-api
	@echo $(DONE_MESSAGE)

run-unit-test: _check_mvn_software_exists
	@echo $(YELLOW)$(INFO_HEADER) "Running unit tests" $(END)
	@$(MVNW_EXE) clean package
	@echo $(DONE_MESSAGE)

demo-start:
	@echo $(YELLOW)$(INFO_HEADER) "Starting demo"$(END)
	@$(DOCKER_COMPOSE_COMMAND) up --build -d
	@echo $(DONE_MESSAGE)

demo-ps:
	@echo $(YELLOW)$(INFO_HEADER) "Showing running services"$(END)
	@$(DOCKER_COMPOSE_COMMAND) ps
	@echo $(DONE_MESSAGE)

demo-logs:
	@echo $(YELLOW)$(INFO_HEADER) "Showing logs for demo services"$(END)
	@$(DOCKER_COMPOSE_COMMAND) logs
	@echo $(DONE_MESSAGE)

demo-stop:
	@echo $(YELLOW)$(INFO_HEADER) "Stopping demo"$(END)
	@$(DOCKER_COMPOSE_COMMAND) down
	@echo $(DONE_MESSAGE)

