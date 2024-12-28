/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.misc

import android.content.Context
import androidx.annotation.StringRes
import kotlinx.serialization.Serializable

/** String based on string or resource id.
 *
 * If sometimes predefined values are needed, which are based on resource id is used and sometimes
 * user defined values are used which are defined by a string, this class can simplify the usage.
 *
 * @param string String or null if resId is not null.
 * @param resId String resource id or null if string is not null.
 */
@Serializable
data class StringOrResId(
    val string: String?,
    @StringRes val resId: Int?
) {
    /** Create a new value based on an explicit string.
     * @param string String as underlying value.
     */
    constructor(string: String) : this(string, null)
    /** Create a new value based on an string resource id.
     * @param resId Resource id as underlying value.
     */
    constructor(@StringRes resId: Int) : this(null, resId)

    /** Get underlying value.
     * @param context Context to resolve resource ids. Can be null, when the object is not
     *   based on resource ids.
     * @return String.
     */
    fun value(context: Context?):  String {
        return if (string != null) {
            string
        } else if (resId != null && context != null) {
            context.getString(resId)
        } else if (resId != null) {
            throw RuntimeException(
                "StringOrResId: Value based on resource id needs context for"
                + " resolving the string."
            )
        } else {
            throw RuntimeException("StringOrResId: No valid string available.")
        }
    }
}
