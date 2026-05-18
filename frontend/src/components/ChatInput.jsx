import { useState } from 'react'

export default function ChatInput({ onSend, disabled, isEmptyState = false }) {
  const [input, setInput] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!input.trim() || disabled) return
    onSend(input.trim())
    setInput('')
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit(e)
    }
  }

  if (isEmptyState) {
    // ChatGPT-style large centered input
    return (
      <div className="w-full">
        <form onSubmit={handleSubmit} className="relative">
          <div className="relative flex items-center bg-gray-700 rounded-[26px] border border-gray-600
                          shadow-lg hover:border-gray-500 focus-within:border-gray-500 transition-colors">
            <button
              type="button"
              className="absolute left-4 text-gray-400 hover:text-gray-300 text-xl"
            >
              +
            </button>
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="有问题，尽管问"
              disabled={disabled}
              rows={1}
              className="flex-1 resize-none bg-transparent px-14 py-4 text-gray-100 placeholder-gray-500
                         focus:outline-none disabled:text-gray-500"
            />
            <button
              type="submit"
              disabled={disabled || !input.trim()}
              className="absolute right-3 w-10 h-10 bg-gray-600 hover:bg-gray-500
                         disabled:bg-gray-700 disabled:cursor-not-allowed
                         rounded-full flex items-center justify-center transition-colors"
            >
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
              </svg>
            </button>
          </div>
        </form>
      </div>
    )
  }

  // Normal compact input at bottom - same capsule style
  return (
    <div className="p-4">
      <form onSubmit={handleSubmit} className="relative">
        <div className="relative flex items-center bg-gray-700 rounded-[26px] border border-gray-600
                        shadow-md hover:border-gray-500 focus-within:border-gray-500 transition-colors">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="输入你的问题..."
            disabled={disabled}
            rows={1}
            className="flex-1 resize-none bg-transparent px-5 py-3.5 text-gray-100 placeholder-gray-500
                       focus:outline-none disabled:text-gray-500"
          />
          <button
            type="submit"
            disabled={disabled || !input.trim()}
            className="absolute right-3 w-9 h-9 bg-gray-600 hover:bg-gray-500
                       disabled:bg-gray-700 disabled:cursor-not-allowed
                       rounded-full flex items-center justify-center transition-colors"
          >
            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
            </svg>
          </button>
        </div>
      </form>
    </div>
  )
}
