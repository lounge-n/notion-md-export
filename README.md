# Notion markdown export
Welcome to Notion markdown export, a tool that allows you to export your Notion documents as Markdown files. With this program, you can easily convert your notes, project plans, and other documents from Notion into a format that is compatible with Hugo, a popular static site generator.  
By using GitHub Actions to automate the export process, you can quickly and easily publish your Notion content on GitHub Pages. Whether you are a blogger, developer, or content creator, notion-md-export can help you streamline your workflow and share your ideas with the world.

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

|  | Property Type       | Required    |
| --- |---------------------|-------------|
| Title | Title               | Yes         |
| Date | Date                | Yes         |
| Slug | Text                | Yes         |
| Publish | Checkbox            | Yes         |
| Tags | MutliSelect         | (Optional)  |
| Categories | MutliSelect         | (Optional)  |
| Description | Text                | (Optional)  |
| Keywords | MutliSelect         | (Optional)  |
| Draft | Boolean             | (Optional)  |
| Aliases | Text or MutliSelect | (Optional)  |

If you set the page cover, it will be set as the "thumbnail" of Front Matter.

## Markdown export path
||Date|Slug|Export path|
| --- | --- | --- | --- |
|1|2022-12-24|About|./content/post/2022/12/24/about/|
|2|2022-12-24|/About|./content/about/|
|3|2022-12-24|(None)|./content/post/2022/12/24/|
|4|(None)|About|./content/about/|
|5|(None)|/About|./content/about/|
|6|(None)|(None)|./content/default/|


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
- [x] Mention page, user
- [x] Date
- [x] Emoticon
- [x] Equation (but requires MathJax)
- [x] Image (Embed and Linked)
- [x] Bookmark
- [x] Embed audio
- [ ] Linked audio (SDK unsupported)
- [ ] Embed video (SDK unsupported)
- [x] Linked video
- [x] Embed file
- [x] Linked file
- [x] Code
