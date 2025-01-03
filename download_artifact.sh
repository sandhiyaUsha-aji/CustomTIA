#!/bin/bash

# 1. Replace these variables with your own details
GITHUB_REPO="your-username/your-repo"  # Replace with your GitHub repo (e.g., "sandhiya/test-repo")
ARTIFACT_NAME="modified-files"
GITHUB_TOKEN="your-personal-access-token"  # Replace with your GitHub PAT
ECLIPSE_PROJECT_DIR="/path/to/your/eclipse/project"  # Replace with your Eclipse project directory

# 2. Fetch the latest workflow run ID
echo "Fetching the latest workflow run ID..."
RUN_ID=$(curl -s \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  "https://api.github.com/repos/$GITHUB_REPO/actions/runs?status=completed" \
  | jq -r ".workflow_runs[0].id")

if [ -z "$RUN_ID" ]; then
  echo "Error: Unable to fetch the workflow run ID."
  exit 1
fi

# 3. Get the artifact download URL
echo "Fetching the artifact download URL..."
DOWNLOAD_URL=$(curl -s \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  "https://api.github.com/repos/$GITHUB_REPO/actions/runs/$RUN_ID/artifacts" \
  | jq -r ".artifacts[] | select(.name==\"$ARTIFACT_NAME\") | .archive_download_url")

if [ -z "$DOWNLOAD_URL" ]; then
  echo "Error: Unable to fetch the artifact download URL."
  exit 1
fi

# 4. Download the artifact
echo "Downloading the artifact..."
curl -L -o artifact.zip \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  "$DOWNLOAD_URL"

# 5. Extract the artifact to the Eclipse project directory
echo "Extracting the artifact to the Eclipse project directory..."
unzip -o artifact.zip -d "$ECLIPSE_PROJECT_DIR"

# 6. Clean up
echo "Cleaning up temporary files..."
rm artifact.zip

echo "Artifact downloaded and placed in $ECLIPSE_PROJECT_DIR successfully!"
