# Acknowledgements
This project is sponsored by **[GitBook](https://www.gitbook.com/)** through their Community plan.
Huge thanks to GitBook for sponsoring open source projects like this one!

# OpenJS

**OpenJS** is a plugin for Minecraft servers (compatible with Bukkit-based APIs including Folia) that allows server logic to be written in JavaScript instead of Java. It acts as an embedded scripting engine with direct integration into the server environment, powered by **GraalVM JavaScript**.

This scripting system exposes parts of the Bukkit API and custom APIs to JavaScript, enabling scripts to create commands, listen to events, and interact with the server in real time. Scripts are loaded and executed without compilation, and can be hot-reloaded without restarting the server.

Scripts run on isolated threads where possible, and are designed to work in modern multithreaded Minecraft environments like Folia. OpenJS supports modern JavaScript (ECMAScript 2022) including async/await, arrow functions, and more. The plugin also supports loading Java libraries and using them from within JavaScript, bridging the gap between the two runtimes.

---

## How it works

OpenJS embeds a JavaScript engine into the server and exposes server-side APIs. Each script file (ending in `.js`) is treated as a runnable module. Scripts can define command handlers, scheduled tasks, event listeners, and more.

---

## Example use cases

- Quickly test and develop logic for custom items, mobs, and mechanics
- Add temporary or experimental features without full Java plugin builds
- Create one-off admin commands or tools for moderation or debugging
- Build scalable logic that works with multithreaded Minecraft (Folia)

---

## Getting started

Documentation for writing scripts, using server APIs, and managing scripts is available at:  
[Documentation](https://docs-mc-1.gitbook.io/openjs-docs)

Need help?  
Join the Discord community: [https://discord.gg/XuRC4at2Ha](https://discord.gg/XuRC4at2Ha)
