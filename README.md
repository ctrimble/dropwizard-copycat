# Dropwizard Copycat

A Copycat Dropwizard bundle and example application.

[![Build Status](https://secure.travis-ci.org/ctrimble/dropwizard-copycat.png?branch=develop)](https://travis-ci.org/ctrimble/dropwizard-copycat)

## Project Status

This project is functional, but is not currently being used in production.

## Project Layout

- `/` The parent project.
- `/bundle` A bundle implementation that provides support for Copycat.
- `/example` A simple example, showing how to use this bundle.

## Build

This project builds with Maven 3.3.9 or later.  Make sure you have Maven installed before building.

To build, clone the repo and run the `install` goal.

```
git clone git@github.com:ctrimble/dropwizard-copycat.git
cd dropwizard-copycat
mvn clean install
```
