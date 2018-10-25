/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine

import android.content.Context
import android.util.AttributeSet
import mozilla.components.concept.engine.file.FilePrompt
import mozilla.components.concept.engine.geo.GeoPrompt
import mozilla.components.concept.engine.window.Window

/**
 * Entry point for interacting with the engine implementation.
 */
interface Engine {

    interface Observer {
        fun onFilePrompt(session: EngineSession, filePrompt: FilePrompt) = Unit
        fun onCreateWindow(session: EngineSession, window: Window) = Unit
        fun onCloseWindow(session:EngineSession, window:Window) = Unit
        fun onShowGeoPermissionPrompt(session: EngineSession, geoPrompt: GeoPrompt)
        fun onHideGeoPermissionPrompt(session: EngineSession, geoPrompt: GeoPrompt)
    }

    /**
     * Creates a new view for rendering web content.
     *
     * @param context an application context
     * @param attrs optional set of attributes
     *
     * @return new newly created [EngineView].
     */
    fun createView(context: Context, attrs: AttributeSet? = null): EngineView

    /**
     * Creates a new engine session.
     *
     * @param private whether or not this session should use private mode.
     *
     * @return the newly created [EngineSession].
     */
    fun createSession(private: Boolean = false): EngineSession

    /**
     * Returns the name of this engine. The returned string might be used
     * in filenames and must therefore only contain valid filename
     * characters.
     *
     * @return the engine name as specified by concrete implementations.
     */
    fun name(): String

    /**
     * Provides access to the settings of this engine.
     */
    val settings: Settings
}
