# RT4 Client, Android

The 2009Scape game client, rewritten to draw through LWJGL and GLFW so it runs on Android under PojavLauncher. This is the client that [DaveRune/2009Scape-mobile](https://github.com/DaveRune/2009Scape-mobile) includes as `rt4.jar`.

I modified this for me, so I could play on my tablet. I am not committing to maintaining it or taking requests. I used AI tools.
I leave it here in case anyone else wants to pick it up one day like I did from downthecrop's work.

## What this is, and what it is not

There are two clients and they are not two versions of one thing.

The **desktop client** at [2009scape/rt4-client](https://gitlab.com/2009scape/rt4-client) draws through JOGL. It is the live one, actively worked on, and a jar built from it will work on Android.

The **Android client** is this. In July 2023 downthecrop took a copy of the desktop client and replaced JOGL with LWJGL 3 and GLFW, because that is what PojavLauncher provides. That work covers the whole renderer: every `Gl*` class, the material renderers, lighting, shadows, fog, water, fonts, sprites, textures and vertex buffers, plus the mouse and keyboard bridge into AWT and an OpenAL audio channel. It stopped in June 2024.

This fork carries that forward. The desktop client's changes since June 2024 are merged in, and the world map is fixed.

## Changes in this fork

| | |
|---|---|
| World map | `GlRaster.drawPixels` used `glRasterPos2i`, `glPixelZoom` and `glDrawPixels`, none of which exist in OpenGL ES, so the map drew as a black rectangle and left stale GL state that tinted the interface afterwards. It now uploads a texture and draws a quad. |
| Build target | Compiles against the Java 8 API rather than only emitting Java 8 bytecode. See below. |
| Build stamp | Every jar records the commit it was built from. |
| Merged | The desktop client's work from June 2024 to August 2026, minus a per-frame framebuffer readback that exists for a desktop overlay plugin and is far too expensive here. |

## Building

```bash
./gradlew :client:jar
```

The output is `client/build/libs/client-1.0.0.jar`. A JDK 17 is fine.

**The build must compile against the Java 8 API, not just emit Java 8 bytecode.** `client/build.gradle` and `signlink/build.gradle` set `options.release = 8` for this. Without it, javac links against the modern class library and emits calls like `ByteBuffer.flip()` returning `ByteBuffer`, which is the Java 9 signature. PojavLauncher runs the client on a Java 8 runtime, so those calls do not exist and the client dies with `NoSuchMethodError` before it draws anything. Setting `sourceCompatibility` and `targetCompatibility` does not prevent this, which is a good few hours of your life if you find out the hard way.

Every jar records its own origin, so any build can be traced back to source:

```bash
unzip -p rt4.jar META-INF/MANIFEST.MF
```

A build made with uncommitted changes is marked `-dirty`.

## Contributing

If you want to contribute to 2009Scape, do it [upstream](https://gitlab.com/2009scape/rt4-client) rather than here, and read their contribution and AI policy first, because they are strict about both and it is their project to run as they see fit.

## Licence

AGPL-3.0, inherited from upstream. See [LICENSE](LICENSE).

| | |
|---|---|
| Forked from | [downthecrop/rt4-client](https://gitlab.com/downthecrop/rt4-client), branch `lwjgl-mobile-callbacks` |
| Which forked | [2009scape/rt4-client](https://gitlab.com/2009scape/rt4-client) |
| Which forked | [Pazaz/RT4-Client](https://github.com/pazaz/rt4-client) |
