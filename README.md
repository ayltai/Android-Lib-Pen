Samsung S-Pen SDK wrapper library
===============================
A Samsung S-Pen library for Android that handles most of the common use cases with a single class only with much reduced complexity compared with Samsung SDK Pen package.

<img src="https://github.com/ayltai/Android-Lib-Pen/demo/screenshot" width="180" height="320" />

Features
--------
* Pen of various styles, colors and adjustable size
* Eraser of adjustable size
* Undo, redo
* Zoom
* Scroll automatically when the pen is hovering near an edge of the canvas
* Replay of strokes with different speed
* Instance thumbnail generation
* Multi-page support
* Re-order pages
* Color background
* Image background
* Supported events: `onTouch`, `onReplayCompleted`, `onPageUpdated`, `onCommit`, `undo`, `redo`
* Draw using S-Pen or finger

Getting Started
---------------
Your target users will need to install the S-Pen SDK package before they can use any Pen features. In the official sample code, the user will be redirected to Google Play Store to download the SDK package developed by Samsung. This library handles this for you and prompt the user for any necessary action.

**Note**: If you plan to submit your app to Samsung Seller Office, they may sometimes reject your submission because your app links to external store (i.e. download Samsung Pen SDK from Google Play Store). You will need to explain to them that this is required as documented in their official sample code.

Using the wrapper
-----------------

**Initialization**

`PenService.init(canvasWidth, canvasHeight, backgroundColor, backgroundImagePath, fittingMode, initialPenColor, initialPenSize, initialEraserSize);`

* You need to create a drawing canvas of a specific dimensions. The dimension cannot be changed later.
* You can either specify a background color or background image but not both. The background color is ignored if a background image is specified.
* `fittingMode` specifies how to fit the background image (best-fit, scaled, cropped, etc.)
* You need to specify the initial pen color and size, and eraser size.

**Create a new drawing**

`PenService.appendPage(backgroundColor, backgroundImagePath, fittingMode);`

* You need to append a page before you can draw on the canvas.

**Load an existing drawing**

`PenService.load(spdFilePath, writable);`

* This will load a .spd file to the canvas
* If you want the drawing to be read-only, set `writable` to `false`. This is useful if you want to replay the strokes only.
* Setting `writable` to `true` will initialize the history stack, which consumes more RAM so use this wisely.

**Drawing**

After you created a new drawing or loaded an existing drawing, you are ready to drawing on it. Users can draw using S-Pen, if any. For devices without S-Pen, they can draw using their fingers.

**Tools**

This library provides various ready-to-use tools, including pen, eraser, undo, redo, zoom, and replay. You can use these tools by using the provided UI, or by invoking the APIs directly.

Demo
----
A <a href="https://github.com/ayltai/Android-Lib-Pen/demo">demo</a> app is provided so you know what you can expect from this library.

This demo shows you how to:

* Create a new drawing
* Load an existing drawing
* Update thumbnail instantly
* Undo, redo
* Save any changes