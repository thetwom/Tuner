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
package de.moekadu.tuner.temperaments

import de.moekadu.tuner.notenames.NoteNames
import kotlinx.serialization.Serializable

//@Serializable
//data class TemperamentWithNoteNames2(
//    val temperament: Temperament2,
//    val noteNames: NoteNames?
//) {
//    val stableId get() = temperament.stableId
//    fun clone(newStableId: Long): TemperamentWithNoteNames2 {
//        return TemperamentWithNoteNames2(
//            temperament = temperament.copy(stableId = newStableId),
//            noteNames = noteNames
//        )
//    }
//}

@Serializable
data class TemperamentWithNoteNames(
    val temperament: Temperament,
    val noteNames: NoteNames?
) {
    fun toNew(): Temperament3Custom {
        return Temperament3Custom(
            _name = temperament.name.value(null),
            _abbreviation = temperament.abbreviation.value(null),
            _description = temperament.description.value(null),
            cents = temperament.cents,
            _rationalNumbers = arrayOf(),
            _noteNames = noteNames?.notes,
            stableId = temperament.stableId
        )
    }
}