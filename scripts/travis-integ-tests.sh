#!/bin/bash
set -e

# This script runs the integration tests against multiple Gradle versions.
# It is used in the travis 'script' step (see ../.travis.yml).


# Determine the 'versions_array' that contains all Gradle versions we have
# to run the integration tests against in this job.
if [[ "${GRADLE_VERSIONS=unset}" == "unset" ]]; then
	# The GRADLE_VERSIONS to test against in this job are _not_ set explicitly.
	# This means we test against some versions from ALL_GRADLE_VERSIONS, for which
	# this specific job is responsible (when testing ALL_GRADLE_VERSIONS
	# tests are split among multiple jobs for performance reasons).
	IFS=',' read -r -a all_versions_array <<< "$ALL_GRADLE_VERSIONS"
	length=$(( (${#all_versions_array[@]} + PARALLEL_INTEG_TEST_COUNT - 1) / PARALLEL_INTEG_TEST_COUNT ))
	versions_array=(${all_versions_array[@]:((INDEX * length)):length})
else
	# The GRADLE_VERSIONS to test against _are_ set explicitly (used to only run
	# integration tests against some gradle versions when varying JDK).
	IFS=',' read -r -a versions_array <<< "$GRADLE_VERSIONS"
fi

echo
echo
echo "*********************************************************************************"
echo "This job runs the integration tests against the following versions of Gradle:"
joint_versions=$(printf ", %s" "${versions_array[@]}")
echo "${joint_versions:2}"
echo "*********************************************************************************"
echo
echo

set -o xtrace
for element in "${versions_array[@]}"; do
	GRADLE_VERSION="$element" ./gradlew integrationTest --exclude-task publishToMavenLocal
done
