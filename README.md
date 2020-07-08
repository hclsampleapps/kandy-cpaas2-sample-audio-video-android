# kandy-cpaas2-sample-audio-video-android
It is the sample app of CPaaS Call module (Voice & Video)

### Install as Android App
You can download the .apk file from [releases](https://github.com/hclsampleapps/kandy-cpaas2-sample-audio-video-android/releases) section.
If you are a developer, then you can generate the apk from source code.

Note: Minimum version required for install apk is kitkat (4.4)

### Run this Android App
1. Open Android App
2. Enter the credentials of a user.

### Execute commands for development
1. Setup repository via `git clone https://github.com/hclsampleapps/kandy-cpaas2-sample-audio-video-android`
2. Resolve build dependency via sync gradle in android studio
3. Generate final build by android studio

### Branching strategy
We are following **GitFlow** as the branching strategy and for release management.

The central repo holds two main branches with an infinite lifetime:

- master
- develop

The `master` branch at origin should be familiar to every Git user. Parallel to the `master` branch, another branch exists called `develop`.

We consider `origin/master` to be the main branch where the source code of HEAD always reflects a *production-ready* state.

We consider `origin/develop` to be the main branch where the source code of HEAD always reflects a state with the latest delivered development changes for the next release.

#### Supporting branches
Next to the main branches `master` and `develop`, our development model uses a variety of supporting branches to aid parallel development between team members.

The different types of branches we may use are:

- Feature branches
- Release branches
- Hotfix branches

### Contributing
Fork the repository. Then, run:

```
git clone --recursive git@github.com:<username>/gitflow.git
cd kandy-cpaas2-sample-android
git branch master origin/master
git flow init -d
git checkout develop
git flow feature start <your feature>
```

Then, do work and commit your changes. When your `feature` is completed, raise the pull-request against `develop`.

To know more about *GitFlow*, please refer

- [Introducing GitFlow](https://datasift.github.io/gitflow/IntroducingGitFlow.html)
- [A successful Git branching model](https://nvie.com/posts/a-successful-git-branching-model/)

### Coding conventions

Contributors should strictly follow JAVA standard conventions:

1. Class names are in camelCase
2. Package names start with lowerCase
3. Variable names are in camelCase
