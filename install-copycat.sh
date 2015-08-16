#!/usr/bin/env sh
git clone https://github.com/kuujo/copycat.git
cd copycat
mvn clean install -Dmaven.test.skip=true
