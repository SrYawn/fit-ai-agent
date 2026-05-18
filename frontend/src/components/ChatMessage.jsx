import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

export default function ChatMessage({ message }) {
  const isUser = message.role === 'user'

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`max-w-[80%] rounded-2xl px-4 py-3 shadow-sm ${
        isUser
          ? 'bg-neutral-100 text-neutral-800'
          : 'bg-white border border-neutral-200 text-neutral-800'
      }`}>
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <div className="prose prose-sm max-w-none prose-p:my-1 prose-headings:my-2">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {message.content || ''}
            </ReactMarkdown>
          </div>
        )}
      </div>
    </div>
  )
}
