#!/usr/bin/env bash
# ============================================================
# GITHUB REPOSITORY SETTINGS
# ============================================================
#
# Applies the repository's settings from this folder, so they live in Git
# instead of in someone's shell history or in the Settings pages:
#
#   rulesets/<name>.json   branch rulesets, created or updated by name
#   this script            merge options and security settings
#
# Run from the project root in Git Bash, logged in to gh as a repository admin:
#
#   bash .github/apply-settings.sh
#
# Safe to run again: every call sets a desired state instead of applying a change,
# so a second run changes nothing. Change a setting here or in a ruleset file,
# commit, then run the script. Never change these settings only in the UI.

# Stop at the first failing command (-e), on an unset variable (-u), and when any
# command in a pipe fails, not only the last one (-o pipefail).
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
  if [ -n "$id" ]; then
    gh api --method PUT "repos/$repo/rulesets/$id" --input "$file" --silent
    echo "Ruleset $name: updated ($id)"
  else
    gh api --method POST "repos/$repo/rulesets" --input "$file" --silent
    echo "Ruleset $name: created"
  fi
done

# 2. Merge options and secret scanning.
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

# 3. Dependabot: alerts on, automatic security updates off.
#    Alerts report dependencies with known vulnerabilities. Renovate opens the
#    update pull requests, so Dependabot's own pull requests would only duplicate them.
gh api --method PUT "repos/$repo/vulnerability-alerts" --silent
gh api --method DELETE "repos/$repo/automated-security-fixes" --silent
echo "Dependabot alerts on, Dependabot security updates off"

# 4. Private vulnerability reporting: lets anyone report a vulnerability privately
#    from the Security tab, as SECURITY.md asks, instead of in a public issue.
gh api --method PUT "repos/$repo/private-vulnerability-reporting" --silent
echo "Private vulnerability reporting: on"

# 5. CodeQL default setup: GitHub's static analysis, run on pushes and pull requests
#    without a workflow file in this repository.
#    - java-kotlin: the backend. Analysed without compiling (build mode none).
#    - actions: the workflows, for problems such as script injection.
gh api --method PATCH "repos/$repo/code-scanning/default-setup" --silent \
  -f state=configured \
  -f query_suite=default \
  -f 'languages[]=java-kotlin' \
  -f 'languages[]=actions'
echo "CodeQL default setup: configured (the first analysis can take a few minutes)"
