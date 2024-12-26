[python-gitlab](https://python-gitlab.readthedocs.io/en/stable/index.html)包文档



# project

```json
{
    "id": 2016,
    "description": "pipeline-demo",
    "name": "pipeline-demo",
    "name_with_namespace": "chengxingfu / pipeline-demo",
    "path": "pipeline-demo",
    "path_with_namespace": "chengxingfu/pipeline-demo",
    "created_at": "2024-11-19T09:44:09.388Z",
    "default_branch": "master",
    "tag_list": [], ❤️
    "ssh_url_to_repo": "git@192.168.2.114:chengxingfu/pipeline-demo.git",
    "http_url_to_repo": "http://192.168.2.114/chengxingfu/pipeline-demo.git",
    "web_url": "http://192.168.2.114/chengxingfu/pipeline-demo",
    "readme_url": "http://192.168.2.114/chengxingfu/pipeline-demo/blob/master/README.md",
    "avatar_url": null,
    "star_count": 0,
    "forks_count": 0,
    "last_activity_at": "2024-11-27T07:35:14.243Z",
    "namespace": {
        "id": 511,
        "name": "chengxingfu",
        "path": "chengxingfu",
        "kind": "user",
        "full_path": "chengxingfu",
        "parent_id": null
    },
    "_links": {
        "self": "http://192.168.2.114/api/v4/projects/2016",
        "issues": "http://192.168.2.114/api/v4/projects/2016/issues",
        "merge_requests": "http://192.168.2.114/api/v4/projects/2016/merge_requests",
        "repo_branches": "http://192.168.2.114/api/v4/projects/2016/repository/branches",
        "labels": "http://192.168.2.114/api/v4/projects/2016/labels",
        "events": "http://192.168.2.114/api/v4/projects/2016/events",
        "members": "http://192.168.2.114/api/v4/projects/2016/members"
    },
    "archived": false,
    "visibility": "private",
    "owner": {
        "id": 426,
        "name": "chengxingfu",
        "username": "chengxingfu",
        "state": "active",
        "avatar_url": "http://192.168.2.114/uploads/-/system/user/avatar/426/avatar.png",
        "web_url": "http://192.168.2.114/chengxingfu"
    },
    "resolve_outdated_diff_discussions": false,
    "container_registry_enabled": true,
    "issues_enabled": true,
    "merge_requests_enabled": true,
    "wiki_enabled": true,
    "jobs_enabled": true,
    "snippets_enabled": true,
    "shared_runners_enabled": true,
    "lfs_enabled": true,
    "creator_id": 426,
    "import_status": "none",
    "import_error": null,
    "open_issues_count": 0,
    "runners_token": "q5xFukpjj9W8kBPKzzX4",
    "public_jobs": true,
    "ci_config_path": null,
    "shared_with_groups": [],
    "only_allow_merge_if_pipeline_succeeds": false,
    "request_access_enabled": false,
    "only_allow_merge_if_all_discussions_are_resolved": false,
    "printing_merge_request_link_enabled": true,
    "merge_method": "merge",
    "permissions": {
        "project_access": {
            "access_level": 40,
            "notification_level": 3
        },
        "group_access": null
    }
}
```

## branch

### crud

```python
project.branches.create({'branch': 'feature1','ref': 'master'})
project.branches.delete('feature1')
project.branches.list()
```

### 属性

```json
{
    "name": "dev",
    "commit": {
        "id": "046bd35d459da20495f77678f02f3e6760b1052c",
        "short_id": "046bd35d", ❤️
        "title": "Initial commit",
        "created_at": "2024-11-19T17:44:10.000+08:00",
        "parent_ids": null,
        "message": "Initial commit",
        "author_name": "chengxingfu",
        "author_email": "chengxf@yusur.tech",
        "authored_date": "2024-11-19T17:44:10.000+08:00",
        "committer_name": "chengxingfu",
        "committer_email": "chengxf@yusur.tech",
        "committed_date": "2024-11-19T17:44:10.000+08:00"
    },
    "merged": false,
    "protected": false,
    "developers_can_push": false,
    "developers_can_merge": false,
    "can_push": true
}


{
    "name": "master",
    "commit": {
        "id": "046bd35d459da20495f77678f02f3e6760b1052c",
        "short_id": "046bd35d",
        "title": "Initial commit",
        "created_at": "2024-11-19T17:44:10.000+08:00",
        "parent_ids": null,
        "message": "Initial commit",
        "author_name": "chengxingfu",
        "author_email": "chengxf@yusur.tech",
        "authored_date": "2024-11-19T17:44:10.000+08:00",
        "committer_name": "chengxingfu",
        "committer_email": "chengxf@yusur.tech",
        "committed_date": "2024-11-19T17:44:10.000+08:00"
    },
    "merged": false,
    "protected": true,
    "developers_can_push": false,
    "developers_can_merge": false,
    "can_push": true
}
```

## tag

### crud

```python
project.tags.list()
project.tags.create({'tag_name':'1.0', 'ref':'master'})
<class 'gitlab.v4.objects.tags.ProjectTag'> => {'name': '1.1', 'message': '', 'target': '6fb51ca26f1f535a34fd96e79a3d3de006b18c85', 'commit': {'id': '6fb51ca26f1f535a34fd96e79a3d3de006b18c85', 'short_id': '6fb51ca2', 'title': 'project.commits.create创建的提交', 'created_at': '2024-12-06T08:33:30.000Z', 'parent_ids': ['e16cb5b213760a8f8a8901af3decaab7eb7d7c2b'], 'message': 'project.commits.create创建的提交', 'author_name': 'chengxingfu', 'author_email': 'chengxf@yusur.tech', 'authored_date': '2024-12-06T08:33:30.000Z', 'committer_name': 'chengxingfu', 'committer_email': 'chengxf@yusur.tech', 'committed_date': '2024-12-06T08:33:30.000Z'}, 'release': None}
```

### 属性

```json
{
    "name": "1.0",
    "message": "",
    "target": "046bd35d459da20495f77678f02f3e6760b1052c",
    "commit": {
        "id": "046bd35d459da20495f77678f02f3e6760b1052c",
        "short_id": "046bd35d",
        "title": "Initial commit",
        "created_at": "2024-11-19T09:44:10.000Z",
        "parent_ids": [],
        "message": "Initial commit",
        "author_name": "chengxingfu",
        "author_email": "chengxf@yusur.tech",
        "authored_date": "2024-11-19T09:44:10.000Z",
        "committer_name": "chengxingfu",
        "committer_email": "chengxf@yusur.tech",
        "committed_date": "2024-11-19T09:44:10.000Z"
    },
    "release": null
}
```

## commits

### crud



1. action选项

[action详情文档](https://docs.gitlab.com/ee/api/commits.html#create-a-commit-with-multiple-files-and-actions)

create：新建文件

delete：删除文件

move：移动

update：更新

chmod

2. file_path 文件全路径
3. content  新内容



```python
commits = project.commits.list()

# 创建一个新文件
data = {
        'branch': 'dev',  #指定在哪个分之上提交
        'commit_message': 'project.commits.create创建的提交',
        'actions': [
            {
                'action': 'create', #❤️ 这里是create 
                'file_path': 'README.rst',
                'content': open('/Users/chengxingfu/code/yusur/hados/pipeline-demo/README.md').read(),
            }
        ]
    }
    commit = project.commits.create(data)
    print(commit.to_json())
    
    
{
    "id": "e16cb5b213760a8f8a8901af3decaab7eb7d7c2b",
    "short_id": "e16cb5b2",
    "title": "project.commits.create创建的提交",
    "created_at": "2024-12-06T08:23:05.000Z",
    "parent_ids": [
        "046bd35d459da20495f77678f02f3e6760b1052c"
    ],
    "message": "project.commits.create创建的提交",
    "author_name": "chengxingfu",
    "author_email": "chengxf@yusur.tech",
    "authored_date": "2024-12-06T08:23:05.000Z",
    "committer_name": "chengxingfu",
    "committer_email": "chengxf@yusur.tech",
    "committed_date": "2024-12-06T08:23:05.000Z",
    "status": null,
    "last_pipeline": null,
    "project_id": 2016
}    

# 更新一个文件
data = {
        'branch': 'dev',  #指定在哪个分之上提交
        'commit_message': 'project.commits.create创建的提交',
        'actions': [
            {
                'action': 'update', #❤️这里是update
                'file_path': 'README.md',
                'content': open('/Users/chengxingfu/code/yusur/hados/pipeline-demo/README.md').read(),
            }
        ]
    }
    commit = project.commits.create(data)

{
    "id": "6fb51ca26f1f535a34fd96e79a3d3de006b18c85",
    "short_id": "6fb51ca2",
    "title": "project.commits.create创建的提交",
    "created_at": "2024-12-06T08:33:30.000Z",
    "parent_ids": [
        "e16cb5b213760a8f8a8901af3decaab7eb7d7c2b"
    ],
    "message": "project.commits.create创建的提交",
    "author_name": "chengxingfu",
    "author_email": "chengxf@yusur.tech",
    "authored_date": "2024-12-06T08:33:30.000Z",
    "committer_name": "chengxingfu",
    "committer_email": "chengxf@yusur.tech",
    "committed_date": "2024-12-06T08:33:30.000Z",
    "status": null,
    "last_pipeline": null,
    "project_id": 2016
}
```

