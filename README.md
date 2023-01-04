# Notion markdown export
💡 **This action is still in testing and is not recommended.**

This action will export a Notion database page to your GitHub repository in Markdown format.

## Usage

### Example Workflow file

```yaml
name: Notion to Markdown

on:
  workflow_dispatch:

jobs:
  export_markdown:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
      with:
        fetch-depth: 0
    - uses: lounge-n/notion-md-export@main
      with:
        notion_auth_token: ${{ secrets.NOTION_AUTH_TOKEN }}
        notion_database_id: ${{ secrets.NOTION_DATABASE_ID }}
    - name: Diff
      id: diff
      run: |
        git add -N .
        git diff --name-only --quiet
      continue-on-error: true
    - name: Commit & Push
      run: |
        set -x
        git config user.name github-actions[bot]
        git config user.email 41898282+github-actions[bot]@users.noreply.github.com
        git add .
        git commit --author=. -m 'update contents'
        git push
      if: steps.diff.outcome == 'failure'
```

### Inputs

| Name |  |  |
| --- | --- | --- |
| notion_auth_token | required | Auth token of the Notion. |
| notion_database_id | required | ID of the Notion database to be exorted. |

### Output

None.

### Notion database properties

|  | Property Type | Required |
| --- | --- | --- |
| Title | Title | Yes |
| Date | Date | Yes |
| Slug | Text | Yes |
| Publish | Checkbox | Yes |
| Tags | MutliSelect | (Optional) |

## Markdown export path

| Date / Slug | About | /About | (None) |
| --- | --- | --- | --- |
| 2022-12-24 | ./content/post/2022/12/24/about/ | ./content/about/ | ./content/post/2022/12/24/ |
| (None) | ./content/about/ | ./content/about/ | ./content/default/ |

## Usage CLI

This script can also be used on the command line.
Requires Kotlin.

```bash
$ ./notion-md-export.kts <Notion auth token> <Notion database id>
```

## ToDo(Support blocks)
- [x] Text paragraph
- [x] Embed page
- [x] ToDo list
- [x] Heading 1, 2, 3
- [x] Simple table(Column list)
- [x] Bulleted list & Numbered list
- [x] Toggle list
- [x] Quote
- [x] Divider
- [x] Call out
- [ ] Mention page, user
- [x] Date
- [x] Emoticon
- [x] Equation (but requires MathJax)
- [x] Image (Embed and Linked)
- [x] Bookmark
- [x] Embed audio
- [ ] Linked audio (SDK not support)
- [ ] Embed video (SDK not support)
- [x] Linked video
- [x] Embed file
- [x] Linked file
- [x] Code
