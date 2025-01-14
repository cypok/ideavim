/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.options

import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.parseNumber
import java.util.*

// Note that we don't want a sealed hierarchy, so we can add options with custom validation
public abstract class Option<T : VimDataType>(public val name: String, public val abbrev: String, public open val defaultValue: T) {
  private val listeners = mutableSetOf<OptionChangeListener<VimDataType>>()

  public open fun addOptionChangeListener(listener: OptionChangeListener<VimDataType>) {
    listeners.add(listener)
  }

  public open fun removeOptionChangeListener(listener: OptionChangeListener<VimDataType>) {
    listeners.remove(listener)
  }

  public fun onChanged(scope: OptionScope, oldValue: VimDataType) {
    for (listener in listeners) {
      when (scope) {
        is OptionScope.GLOBAL -> listener.processGlobalValueChange(oldValue)
        is OptionScope.LOCAL -> {
          if (listener is LocalOptionChangeListener) {
            listener.processLocalValueChange(oldValue, scope.editor)
          } else {
            listener.processGlobalValueChange(oldValue)
          }
        }
      }
    }
  }

  // todo 1.9 should return Result with exceptions
  public abstract fun checkIfValueValid(value: VimDataType, token: String)
  public abstract fun parseValue(value: String, token: String): VimDataType
}

public open class StringOption(name: String, abbrev: String, defaultValue: VimString, public val isList: Boolean = false, public val boundedValues: Collection<String>? = null) : Option<VimString>(name, abbrev, defaultValue) {
  public constructor(name: String, abbrev: String, defaultValue: String, isList: Boolean = false, boundedValues: Collection<String>? = null) : this(name, abbrev, VimString(defaultValue), isList, boundedValues)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimString) {
      throw exExceptionMessage("E474", token)
    }

    if (value.value.isEmpty()) {
      return
    }

    if (boundedValues != null && split(value.value).any { !boundedValues.contains(it) }) {
      throw exExceptionMessage("E474", token)
    }
  }

  override fun parseValue(value: String, token: String): VimString =
    VimString(value).also { checkIfValueValid(it, token) }

  public fun appendValue(currentValue: VimString, value: VimString): VimString {
    if (split(currentValue.value).contains(value.value)) return currentValue
    return VimString(joinValues(currentValue.value, value.value))
  }

  public fun prependValue(currentValue: VimString, value: VimString): VimString {
    if (split(currentValue.value).contains(value.value)) return currentValue
    return VimString(joinValues(value.value, currentValue.value))
  }

  public fun removeValue(currentValue: VimString, value: VimString): VimString {
    val newValue = if (isList) {
      val valuesToRemove = split(value.value)
      val elements = split(currentValue.value).toMutableList()
      if (Collections.indexOfSubList(elements, valuesToRemove) != -1) {
        // see `:help set`
        // When the option is a list of flags, {value} must be
        // exactly as they appear in the option.  Remove flags
        // one by one to avoid problems.
        elements.removeAll(valuesToRemove)
      }
      elements.joinToString(separator = ",")
    } else {
      // TODO: Not sure this is correct. Should replace just the first occurrence?
      currentValue.value.replace(value.value, "")
    }
    return VimString(newValue)
  }

  public open fun split(value: String): List<String> {
    return if (isList) {
      value.split(",")
    } else {
      listOf(value)
    }
  }

  private fun joinValues(first: String, second: String): String {
    val separator = if (isList && first.isNotEmpty()) "," else ""
    return first + separator + second
  }
}

public open class NumberOption(name: String, abbrev: String, defaultValue: VimInt) :
  Option<VimInt>(name, abbrev, defaultValue) {
  public constructor(name: String, abbrev: String, defaultValue: Int) : this(name, abbrev, VimInt(defaultValue))

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E521", token)
  }

  override fun parseValue(value: String, token: String): VimInt =
    VimInt(parseNumber(value) ?: throw exExceptionMessage("E521", token)).also { checkIfValueValid(it, token) }

  public fun addValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value + value2.value)
  public fun multiplyValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value * value2.value)
  public fun subtractValues(value1: VimInt, value2: VimInt): VimInt = VimInt(value1.value - value2.value)
}

public open class UnsignedNumberOption(name: String, abbrev: String, defaultValue: VimInt) :
  NumberOption(name, abbrev, defaultValue) {

  public constructor(name: String, abbrev: String, defaultValue: Int) : this(name, abbrev, VimInt(defaultValue))

  override fun checkIfValueValid(value: VimDataType, token: String) {
    super.checkIfValueValid(value, token)
    if ((value as VimInt).value < 0) {
      throw ExException("E487: Argument must be positive: $token")
    }
  }
}

public class ToggleOption(name: String, abbrev: String, defaultValue: VimInt) : Option<VimInt>(name, abbrev, defaultValue) {
  public constructor(name: String, abbrev: String, defaultValue: Boolean) : this(name, abbrev, if (defaultValue) VimInt.ONE else VimInt.ZERO)

  override fun checkIfValueValid(value: VimDataType, token: String) {
    if (value !is VimInt) throw exExceptionMessage("E474", token)
  }

  override fun parseValue(value: String, token: String): Nothing = throw exExceptionMessage("E474", token)
}

public fun Option<out VimDataType>.appendValue(currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (this) {
    is StringOption -> this.appendValue(currentValue as VimString, value as VimString)
    is NumberOption -> this.addValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}

public fun Option<out VimDataType>.prependValue(currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (this) {
    is StringOption -> this.prependValue(currentValue as VimString, value as VimString)
    is NumberOption -> this.multiplyValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}

public fun Option<out VimDataType>.removeValue(currentValue: VimDataType, value: VimDataType): VimDataType? {
  return when (this) {
    is StringOption -> this.removeValue(currentValue as VimString, value as VimString)
    is NumberOption -> this.subtractValues(currentValue as VimInt, value as VimInt)
    else -> null
  }
}
