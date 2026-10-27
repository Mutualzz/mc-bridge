# Contributing

Thanks for wanting to help with Mutualzz Bridge / Mutualzz Voice.

Contributions are welcome. Redistributing unofficial builds or running your
own hub is still not allowed — see [`LICENSE`](https://git.mutualzz.com/mutualzz/mc-bridge/src/branch/master/LICENSE).

## Credit

We want contributors to get credit for their work.

- Your **git author name/email** stays on commits that land
- Merged **pull requests** keep your attribution
- Notable changes are called out in **changelogs / release notes** when we can
- You're welcome to list Mutualzz contributions on your portfolio or resume

If we miss crediting something meaningful, [open an issue](https://git.mutualzz.com/mutualzz/mc-bridge/issues) or mention it on the
PR and we'll fix it.

## How to contribute

1. Sign in to [git.mutualzz.com](https://git.mutualzz.com) with your GitHub account.
2. Fork [the repository](https://git.mutualzz.com/mutualzz/mc-bridge).
3. Create a branch for your change.
4. Make your edits (keep the scope focused).
5. Test locally when you can (`./gradlew jar` / `./gradlew runServer` for the plugin, `./gradlew jar` / `./gradlew runClient` for the mod). Bridge builds emit **Java 17** bytecode (`options.release = 17`) so one jar can run on Paper **1.18.2+**; keep chat listeners dual (Paper `AsyncChatEvent` + Bukkit legacy).
6. Open a pull request on git.mutualzz.com and describe what changed and why.

GitHub is a read-only mirror — pull requests opened there are not monitored.

## Contribution license

By opening a pull request or otherwise submitting a patch, you agree that:

- You have the right to submit the contribution
- You grant Mutualzz a perpetual, worldwide, royalty-free, irrevocable license
  to use, modify, and distribute your contribution as part of Mutualzz
  products under the project license
- Mutualzz will credit you as described under **Credit** above

You do **not** get rights to redistribute Mutualzz builds or host a competing
bridge hub just by contributing.

## What we’re looking for

- Bug fixes and stability improvements
- Clearer player / server-owner docs
- Minecraft version compatibility fixes
- Small, reviewable PRs

## What to avoid

- Publishing your fork’s jars for others to download
- Changing the default hub away from Mutualzz’s official endpoint in a PR
  meant for upstream
- Large unrelated refactors in the same PR as a bug fix

Questions? Reach out via [mutualzz.com](https://mutualzz.com).
