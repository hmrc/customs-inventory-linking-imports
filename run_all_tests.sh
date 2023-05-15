#!/usr/bin/env bash

#sbt clean scalastyle coverage test it:test coverageReport dependencyUpdates
sbt clean scalastyle coverage test it:test dependencyUpdates
