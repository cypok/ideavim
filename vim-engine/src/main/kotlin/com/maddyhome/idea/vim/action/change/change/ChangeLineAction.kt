/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.change.change

import com.maddyhome.idea.vim.action.motion.updown.MotionDownLess1FirstNonSpaceAction
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_MULTIKEY_UNDO
import com.maddyhome.idea.vim.command.CommandFlags.FLAG_NO_REPEAT_INSERT
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.ChangeEditorActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

public class ChangeLineAction : ChangeEditorActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.CHANGE

  override val flags: EnumSet<CommandFlags> = enumSetOf(FLAG_NO_REPEAT_INSERT, FLAG_MULTIKEY_UNDO)

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    argument: Argument?,
    operatorArguments: OperatorArguments,
  ): Boolean {
    // `S` command is a synonym of `cc`
    val motion = MotionDownLess1FirstNonSpaceAction()
    val command = Command(1, motion, motion.type, motion.flags)
    return injector.changeGroup.changeMotion(
      editor,
      caret,
      context,
      Argument(command),
      operatorArguments,
    )
  }
}
