/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.OptionValueAccessor
import com.maddyhome.idea.vim.options.StringOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt

public interface VimOptionGroup {
  /**
   * Get the [Option] by its name or abbreviation
   */
  public fun getOption(key: String): Option<out VimDataType>?

  /**
   * @return list of all options
   */
  public fun getAllOptions(): Set<Option<out VimDataType>>

  /**
   * Get the value for the option in the given scope
   */
  public fun getOptionValue(option: Option<out VimDataType>, scope: OptionScope): VimDataType

  /**
   * Set the value for the option in the given scope
   */
  public fun setOptionValue(option: Option<out VimDataType>, scope: OptionScope, value: VimDataType)

  /**
   * Resets all options back to default values.
   */
  public fun resetAllOptions()

  /**
   * Adds the option.
   * @param option option
   */
  public fun addOption(option: Option<out VimDataType>)

  /**
   * Removes the option.
   * @param optionName option name or alias
   */
  public fun removeOption(optionName: String)

  /**
   * Adds a listener to the option.
   * @param optionName option name or alias
   * @param listener option listener
   * @param executeOnAdd whether execute listener after the method call or not
   */
  public fun addListener(optionName: String, listener: OptionChangeListener<VimDataType>, executeOnAdd: Boolean = false)

  /**
   * Remove the listener from the option.
   * @param optionName option name or alias
   * @param listener option listener
   */
  public fun removeListener(optionName: String, listener: OptionChangeListener<VimDataType>)

  /**
   * Return an accessor class to easily retrieve options values
   *
   * Note that passing `null` as an editor means that you're only interested in global options - NOT global values of
   * local to buffer or local to window or global-local options! For that, use [getOptionValue].
   *
   * @param editor The editor to use to retrieve local option values. If `null`, then only global values are available
   * @return An instance of [OptionValueAccessor] to provide easy API to get option values
   */
  public fun getValueAccessor(editor: VimEditor?): OptionValueAccessor
}

/**
 * Checks if option is set to its default value
 */
public fun VimOptionGroup.isDefaultValue(option: Option<out VimDataType>, scope: OptionScope): Boolean =
  getOptionValue(option, scope) == option.defaultValue

/**
 * Resets the option back to its default value
 */
public fun VimOptionGroup.resetDefaultValue(option: Option<out VimDataType>, scope: OptionScope) {
  setOptionValue(option, scope, option.defaultValue)
}

/**
 * Checks if the given string option matches the value, or a string list contains the value
 */
public fun VimOptionGroup.hasValue(option: StringOption, scope: OptionScope, value: String): Boolean =
  value in option.split(getOptionValue(option, scope).asString())

/**
 * Splits a string list option into flags, or returns a list with a single string value
 *
 * E.g. the `fileencodings` option with value "ucs-bom,utf-8,default,latin1" will result listOf("ucs-bom", "utf-8", "default", "latin1")
 */
public fun VimOptionGroup.getStringListValues(option: StringOption, scope: OptionScope): List<String> {
  return option.split(getOptionValue(option, scope).asString())
}

/**
 * Sets the toggle option on
 */
public fun VimOptionGroup.setToggleOption(option: ToggleOption, scope: OptionScope) {
  setOptionValue(option, scope, VimInt.ONE)
}

/**
 * Unsets a toggle option
 */
public fun VimOptionGroup.unsetToggleOption(option: ToggleOption, scope: OptionScope) {
  setOptionValue(option, scope, VimInt.ZERO)
}

/**
 * Inverts toggle option value, setting it on if off, or off if on.
 */
public fun VimOptionGroup.invertToggleOption(option: ToggleOption, scope: OptionScope) {
  val optionValue = getOptionValue(option, scope)
  setOptionValue(option, scope, if (optionValue.asBoolean()) VimInt.ZERO else VimInt.ONE)
}

/**
 * Get an instance of [Option] for a well-known option name
 *
 * The option must exist, or an exception will be thrown
 */
public fun VimOptionGroup.getKnownOption(optionName: String): Option<out VimDataType> = getOption(optionName)!!

/**
 * Get an instance of [ToggleOption] for a well-known option name
 *
 * The option must exist, or an exception will be thrown
 */
public fun VimOptionGroup.getKnownToggleOption(optionName: String): ToggleOption = getOption(optionName) as ToggleOption

/**
 * Get an instance of [StringOption] for a well-known option name
 *
 * The option must exist, or an exception will be thrown
 */
public fun VimOptionGroup.getKnownStringOption(optionName: String): StringOption = getOption(optionName) as StringOption

/**
 * Modifies the value of an option by calling the given transform function
 */
public inline fun <TDataType : VimDataType> VimOptionGroup.modifyOptionValue(
  option: Option<TDataType>,
  scope: OptionScope,
  transform: (TDataType) -> TDataType?,
) {
  @Suppress("UNCHECKED_CAST")
  val currentValue = getOptionValue(option, scope) as TDataType
  transform(currentValue)?.let {
    setOptionValue(option, scope, it)
  }
}
