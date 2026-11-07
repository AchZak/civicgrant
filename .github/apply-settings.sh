#!/usr/bin/env bash
# ============================================================
# GITHUB REPOSITORY SETTINGS
# ============================================================
#
# Applies the repository's settings from this folder, so they live in Git
# instead of in someone's shell history or in the Settings pages:
#
#   rulesets/<name>.json           branch rulesets, created or updated by name
#   environments/<name>.json       deployment environments (tst, acc, prd) and
#                                  their protection rules
#   environments/<name>.branches   the branches allowed to deploy to each one
#   this script                    merge options and security settings
#
# Run from the project root in Git Bash, logged in to gh as a repository admin:
#
#   bash .github/apply-settings.sh
#
# Safe to run again: every call sets a desired state instead of applying a change,
# so a second run changes nothing. Change a setting here or in a file under
# rulesets/ or environments/, commit, then run the script. Never change these
# settings only in the UI.

# Stop at the first failing command (-e), on an unset variable (-u), and when any
# command in a pipe fails, not only the last one (-o pipefail).
# Tests use bash's [[ ]], not [ ]: a variable inside [[ ]] is never split into
# several words or expanded as a file pattern, even if its quotes are forgotten.
set -euo pipefail


# ------------------------------------------------------------
# VARIABLES
# ------------------------------------------------------------

# owner/name of the repository the current folder belongs to: AchZak/civicgrant.
repo=$(gh repo view --json nameWithOwner --jq .nameWithOwner)


# ------------------------------------------------------------
# SETTINGS
# ------------------------------------------------------------

# 1. Rulesets. Each file's name is its ruleset's name: rulesets/main.json -> "main".
#    An existing ruleset is replaced as a whole (PUT), a missing one is created (POST).
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

# 2. Deployment environments. Each JSON file is one GitHub Environment, named after
#    the file: environments/tst.json -> "tst". A job that declares "environment: tst"
#    gets tst's protection rules and variables, and GitHub's OIDC token for it carries
#    the subject <prefix>:environment:tst, the only one id-civicgrant-deploy-tst trusts.
#    - The JSON file is the body of GitHub's "create or update an environment" call:
#      PUT creates the environment, or replaces the settings of an existing one.
#    - <name>.branches lists the branches allowed to deploy. GitHub stores them as
#      separate "deployment branch policies" that the PUT can't set, and adding one
#      twice is refused. So the file is compared with what exists: missing branches
#      are added, branches no longer in the file are removed.
for file in .github/environments/*.json; do
  name=$(basename "$file" .json)
  gh api --method PUT "repos/$repo/environments/$name" --input "$file" --silent

  # Fresh lists for each environment. Associative arrays, like a Java Map:
  # wanted maps branch -> 1, existing maps branch -> GitHub's policy id.
  unset wanted existing
  declare -A wanted=() existing=()

  # Wanted: the .branches file without comments, blank lines and Windows line endings.
  # Read into a variable first, so set -e stops the script if the file is missing.
  branches=$(sed -e 's/\r$//' -e 's/#.*//' -e 's/[[:space:]]*$//' -e '/^$/d' ".github/environments/$name.branches")
  while IFS= read -r branch; do
    if [[ -n "$branch" ]]; then
      wanted["$branch"]=1
    fi
  done <<< "$branches"

  # Existing: what GitHub has now, as "<id> <branch>" lines.
  policies=$(gh api "repos/$repo/environments/$name/deployment-branch-policies?per_page=100" \
    --jq '.branch_policies[] | "\(.id) \(.name)"' | tr -d '\r')
  # An empty answer still gives one empty line, hence the check on the id.
  while read -r id branch; do
    if [[ -n "$id" ]]; then
      existing["$branch"]=$id
    fi
  done <<< "$policies"

  # Remove what the file no longer lists, then add what's missing.
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

# 3. Merge options and secret scanning.
#    - allow_merge_commit: the main ruleset only accepts merge commits, so they
#      must be allowed at repository level too.
#    - delete_branch_on_merge: off, because merging develop into main must never
#      delete develop. Renovate deletes its own branches.
#    - Secret scanning finds committed credentials; push protection rejects a push
#      that contains one before it reaches GitHub.
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

# 4. Dependabot: alerts on, automatic security updates off.
#    Alerts report dependencies with known vulnerabilities. Renovate opens the
#    update pull requests, so Dependabot's own pull requests would only duplicate them.
gh api --method PUT "repos/$repo/vulnerability-alerts" --silent
gh api --method DELETE "repos/$repo/automated-security-fixes" --silent
echo "Dependabot alerts on, Dependabot security updates off"

# 5. Private vulnerability reporting: lets anyone report a vulnerability privately
#    from the Security tab, as SECURITY.md asks, instead of in a public issue.
gh api --method PUT "repos/$repo/private-vulnerability-reporting" --silent
echo "Private vulnerability reporting: on"

# 6. CodeQL default setup: GitHub's static analysis, run on pushes and pull requests
#    without a workflow file in this repository.
#    - java-kotlin: the backend. Analysed without compiling (build mode none).
#    - actions: the workflows, for problems such as script injection.
gh api --method PATCH "repos/$repo/code-scanning/default-setup" --silent \
  -f state=configured \
  -f query_suite=default \
  -f 'languages[]=java-kotlin' \
  -f 'languages[]=actions'
echo "CodeQL default setup: configured (the first analysis can take a few minutes)"
