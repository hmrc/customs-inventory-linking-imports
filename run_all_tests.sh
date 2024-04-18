#!/usr/bin/env bash

sbt clean scalastyle coverage test IntegrationTest/test coverageReport dependencyUpdates
