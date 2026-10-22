package com.maiasupport.engine;

/** Result callback for asynchronous Maia inference requests. */
oneway interface IMaiaResultCallback {
    void onResult(String requestId, String bestMoveUci, String errorMessage);
}
