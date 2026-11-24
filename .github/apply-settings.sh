#!/usr/bin/env bash
# Applies the repository's settings from rulesets/, environments/ and this script, so they
# live in Git. Idempotent. Run from the repository root as a repository admin:
#   bash .github/apply-settings.sh
set -euo pipefail

repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner)

for file in .github/rulesets/*.json; do
  name=$(basename "$file" .json)
  id=$(gh api "repos/$repo/rulesets" --jq ".[] | select(.name == \"$name\") | .id" | tr -d '\r')
  if [[ -n "$id" ]]; then
    gh api --method PUT "repos/$repo/rulesets/$id" --input "$file" --silent
    echo "Ruleset $name: updated ($id)"
  else
    gh api --method POST "repos/$repo/rulesets" --input "$file" --silent
    echo "Ruleset $name: created"
  fi
done

# The environment PUT can't set deployment branch policies, and adding one twice is
# refused, so each <name>.branches file is compared with what GitHub has.
for file in .github/environments/*.json; do
  name=$(basename "$file" .json)
  gh api --method PUT "repos/$repo/environments/$name" --input "$file" --silent

  unset wanted existing
  declare -A wanted=() existing=()

  # Read into a variable first, so set -e stops the script when the file is missing.
  branches=$(sed -e 's/\r$//' -e 's/#.*//' -e 's/[[:space:]]*$//' -e '/^$/d' ".github/environments/$name.branches")
  while IFS= read -r branch; do
    if [[ -n "$branch" ]]; then
      wanted["$branch"]=1
    fi
  done <<< "$branches"

  policies=$(gh api "repos/$repo/environments/$name/deployment-branch-policies?per_page=100" \
    --jq '.branch_policies[] | "\(.id) \(.name)"' | tr -d '\r')
  # An empty answer still gives one empty line.
  while read -r id branch; do
    if [[ -n "$id" ]]; then
      existing["$branch"]=$id
    fi
  done <<< "$policies"

  for branch in "${!existing[@]}"; do
    if [[ -z "${wanted[$branch]:-}" ]]; then
      gh api --method DELETE "repos/$repo/environments/$name/deployment-branch-policies/${existing[$branch]}" --silent
      echo "Environment $name: branch $branch removed"
    fi
  done
  for branch in "${!wanted[@]}"; do
    if [[ -z "${existing[$branch]:-}" ]]; then
      gh api --method POST "repos/$repo/environments/$name/deployment-branch-policies" \
        -f name="$branch" -f type=branch --silent
      echo "Environment $name: branch $branch added"
    fi
  done
  echo "Environment $name: applied"
done

# The main ruleset only accepts merge commits. Merging develop into main must never
# delete develop; Renovate deletes its own branches.
gh api --method PATCH "repos/$repo" --silent --input - <<'EOF'
{
  "allow_merge_commit": true,
  "delete_branch_on_merge": false,
  "security_and_analysis": {
    "secret_scanning": { "status": "enabled" },
    "secret_scanning_push_protection": { "status": "enabled" }
  }
}
EOF
echo "Merge options and secret scanning: applied"

# Alerts on, but Renovate opens the update pull requests, not Dependabot.
gh api --method PUT "repos/$repo/vulnerability-alerts" --silent
gh api --method DELETE "repos/$repo/automated-security-fixes" --silent
echo "Dependabot alerts on, Dependabot security updates off"

gh api --method PUT "repos/$repo/private-vulnerability-reporting" --silent
echo "Private vulnerability reporting: on"

gh api --method PATCH "repos/$repo/code-scanning/default-setup" --silent \
  -f state=configured \
  -f query_suite=default \
  -f 'languages[]=java-kotlin' \
  -f 'languages[]=actions'
echo "CodeQL default setup: configured (the first analysis can take a few minutes)"
