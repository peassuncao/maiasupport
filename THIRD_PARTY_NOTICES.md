# Third-party notices

## Maia-3 code and models

Maia-3 is the human-like chess move-prediction project from the University of Toronto CSSLab. Its source repository and the official 5M/79M checkpoints identify AGPL-3.0 licensing. MaiaSupport independently implements an Android AIDL provider using the published model input conventions and is distributed under AGPL-3.0-only.

The optional **5M ONNX** artifact is the converted Maia-3 5M model published by MoveAPiece at pinned source revision `5dd3521bc95197e8f778d515e83ba7c66f09d3f3`. The model is 20,990,437 bytes, SHA-256 `f72550295eaaf66d8d8c94c5554cf5c3742dfd07f153d758b0d7baf0206ec589`; technical provenance and parity checks are described in `MODEL_PROVENANCE.md`. Its upstream checkpoint and conversion are licensed AGPL-3.0.

The optional **79M ONNX** artifact is the converted Maia-3 79M model published by Mobile Maia at pinned revision `cd2551f70f9dde903d4bf543c7ea309beecee58f`. It is 316,034,244 bytes, SHA-256 `3454b03ae78baa64a87b345fdb1a457265d912caec531039b074f07eda0d8010`, and is documented as AGPL-3.0. MaiaSupport does not claim either conversion is produced or endorsed by CSSLab. The ONNX model files are not bundled in the APK or source archive; downloads are opt-in and prompted in-app.

Upstream sources: https://github.com/CSSLab/maia3, https://github.com/egdels/moveapiece and https://github.com/Dash1971/maia-chess-android. See `MODEL_PROVENANCE.md` for exact file paths, checkpoint revisions, model interfaces, and URLs.

## ONNX Runtime Android

The app uses `com.microsoft.onnxruntime:onnxruntime-android:1.24.3` from Maven Central, licensed under MIT. Source and license: https://github.com/microsoft/onnxruntime.

## Independence

MaiaSupport is an independent community project. It is not developed, endorsed, or maintained by University of Toronto, CSSLab, Maia Chess, Mobile Maia, or MoveAPiece. The name Maia identifies the compatible upstream engine/model only.
