import type { KeyboardEvent } from 'react'

/**
 * Lets Enter in a text field submit a form whose submit button lives in a second <form> (the dialogs that
 * keep the IT Manager's own password apart from someone else's new password).
 */
export function submitOnEnter(submit: () => unknown) {
  return (event: KeyboardEvent<HTMLFormElement>) => {
    if (event.key !== 'Enter' || event.nativeEvent.isComposing || !(event.target instanceof HTMLInputElement)) return
    event.preventDefault()
    void submit()
  }
}
