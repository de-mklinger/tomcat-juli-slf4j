#!/bin/bash

PREPARE_OPTS=""
if [ "$1" = "-f" ]; then
    PREPARE_OPTS="-f"
fi

set -eu

# TODO
DIR="."

#echo "Fetching..."
#git fetch -q

FAILED_BRANCHES=""

ALL_BRANCHES=$(git for-each-ref refs/heads --format='%(refname:short)' | cut -f 2 | grep -e '^tomcat-juli-slf4j-.*\.x$' -e '^master$')

##'## fix for mc editor, sorry.

declare -A BRANCH_NEXT_VERSIONS

while read BRANCH; do
    echo "$BRANCH"
    echo "  Preparing..."
    git checkout -q "$BRANCH"
    git pull -q
    echo "  Testing..."
    set +e
    OUT="$(mvn clean test -Puptodatetest)"
    if [ $? = 0 ]; then
        set -e
        echo "✓ Success for $BRANCH";
	echo
    else
	set -e
        LINE=`grep "Later version available" <<< $OUT`
	if [ "$LINE" = "" ]; then
            echo "X Error for $BRANCH";
	    cat <<< $OUT
	    echo
	    exit 1
	else
            echo "# Failed for $BRANCH";
	    echo -n "  "
	    cat <<< $LINE
	    echo
	    FAILED_BRANCHES="$FAILED_BRANCHES $BRANCH"
	    # TODO
	    VERSION=$(sed 's/Later version available: \(.*\)/\1/' <<< $LINE)
	    ##'## fix for mc editor, sorry.
	    BRANCH_NEXT_VERSIONS[${BRANCH}]=$VERSION
	fi
    fi
done <<< $ALL_BRANCHES

echo

if [ "$FAILED_BRANCHES" != "" ]; then

 for BRANCH in $FAILED_BRANCHES; do
    echo "$BRANCH"
    echo "  Preparing for preparation..."
    git checkout -q "$BRANCH"
    git pull -q
    echo "  Doing preparation..."
    echo "  Next version: ${BRANCH_NEXT_VERSIONS[${BRANCH}]}"
    (cd "$DIR" && env NEXT_VERSION="${BRANCH_NEXT_VERSIONS[${BRANCH}]}" ./prepare-next.sh ${PREPARE_OPTS})
  done

  echo "Pushing:"

  git push --atomic origin $FAILED_BRANCHES

fi

echo "Done."

