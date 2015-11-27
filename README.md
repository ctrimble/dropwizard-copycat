# Dropwizard Copycat

A Copycat Dropwizard bundle and example application.

[![Build Status](https://secure.travis-ci.org/ctrimble/dropwizard-copycat.png?branch=develop)](https://travis-ci.org/ctrimble/dropwizard-copycat)

## Project Status

This project is not yet functional, nor is it intended for projection use.

## Project Layout

- `/` The parent project.
- `/bundle` A bundle implementation that provides support for Copycat.
- `/example` A simple example, showing how to use this bundle.

## Before you Build

This project requires [Copycat 0.6.0](https://github.com/kuujo/copycat).  You should check this version out locally and build it, before building this project.

## Build

This project builds with Maven 3.1.X or later.  Make sure you have Maven installed before building.

To build, clone the repo and run the `install` goal.

```
git clone git@github.com:ctrimble/dropwizard-copycat.git
cd dropwizard-copycat
mvn clean install
```
