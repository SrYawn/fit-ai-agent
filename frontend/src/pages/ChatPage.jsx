import { useState, useRef, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { connectWorkflowStream } from '../services/sse'
import ThinkingStatus from '../components/ThinkingStatus'
import ChatMessage from '../components/ChatMessage'
import ChatInput from '../components/ChatInput'
import ChatSidebar from '../components/ChatSidebar'

export default function ChatPage({ user, onLogout }) {
  const [messages, setMessages] = useState([])
  const [streaming, setStreaming] = useState(false)
  const [thinkingSteps, setThinkingSteps] = useState([])
  const [streamingContent, setStreamingContent] = useState('')
  const [sessionId, setSessionId] = useState(null)
  const messagesEndRef = useRef(null)
  const eventSourceRef = useRef(null)
  const contentRef = useRef('')
  const navigate = useNavigate()

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, streamingContent, thinkingSteps])

  const handleNewChat = () => {
    if (eventSourceRef.current) eventSourceRef.current.close()
    setMessages([])
    setSessionId(null)
    setStreaming(false)
    setThinkingSteps([])
    setStreamingContent('')
    contentRef.current = ''
  }

  const handleSelectSession = async (selectedSessionId) => {
    if (selectedSessionId === sessionId) return
    if (eventSourceRef.current) eventSourceRef.current.close()
    setStreaming(false)
    setThinkingSteps([])
    setStreamingContent('')
    contentRef.current = ''
    setSessionId(selectedSessionId)

    try {
      const res = await fetch(
        `/api/chat/sessions/${selectedSessionId}/messages?userId=${user.id}`
      )
      const data = await res.json()
      if (data.success) {
        setMessages(
          (data.data || []).map((m) => ({ role: m.role, content: m.content }))
        )
      }
    } catch (err) {
      console.error('加载会话消息失败', err)
    }
  }

  const handleSend = (input) => {
    setMessages(prev => [...prev, { role: 'user', content: input }])
    setStreaming(true)
    setThinkingSteps([])
    setStreamingContent('')
    contentRef.current = ''

    eventSourceRef.current = connectWorkflowStream({
      userInput: input,
      userId: user.id,
      sessionId,
      onMetadata: (data) => {
        if (data.sessionId && !sessionId) {
          setSessionId(data.sessionId)
          // 刷新侧边栏
          if (window.__refreshSessions) window.__refreshSessions()
        }
        setThinkingSteps(prev => {
          const existing = prev.findIndex(s => s.node === data.node)
          if (existing >= 0) {
            const updated = [...prev]
            updated[existing] = { ...updated[existing], ...data }
            return updated
          }
          return [...prev, data]
        })
      },
      onToken: (data) => {
        contentRef.current += (data.content || '')
        setStreamingContent(contentRef.current)
      },
      onDone: (data) => {
        const finalContent = data.content || contentRef.current
        setMessages(prev => [...prev, { role: 'assistant', content: finalContent }])
        setStreamingContent('')
        setStreaming(false)
        setThinkingSteps(prev => prev.map(s => ({ ...s, status: 'completed' })))
        // 刷新侧边栏（更新 lastMessageAt 排序）
        if (window.__refreshSessions) window.__refreshSessions()
      },
      onError: (data) => {
        setMessages(prev => [...prev, { role: 'assistant', content: `错误: ${data.message || '请求失败'}` }])
        setStreamingContent('')
        setStreaming(false)
      }
    })
  }

  const handleLogout = () => {
    if (eventSourceRef.current) eventSourceRef.current.close()
    onLogout()
    navigate('/')
  }

  return (
    <div className="h-screen flex flex-col bg-gray-800">
      {/* Header - full width, spans over sidebar */}
      <header className="bg-gray-850 border-b border-gray-700/50 px-5 py-3 flex items-center justify-between"
              style={{ backgroundColor: '#1a2332' }}>
        <h1 className="text-lg font-semibold text-gray-100 tracking-wide">智能健身助理</h1>
        <div className="flex items-center gap-4">
          {user.role === 'ADMIN' && (
            <Link to="/knowledge" className="text-sm text-gray-300 hover:text-white transition-colors">
              知识库管理
            </Link>
          )}
          <Link to="/records" className="text-sm text-gray-300 hover:text-white transition-colors">
            我的记录
          </Link>
          <span className="text-sm text-gray-500">{user.username}</span>
          <button onClick={handleLogout} className="text-sm text-gray-400 hover:text-gray-200 transition-colors">
            退出
          </button>
        </div>
      </header>

      {/* Body: sidebar + chat */}
      <div className="flex-1 flex overflow-hidden">
        {/* Sidebar */}
        <ChatSidebar
          userId={user.id}
          activeSessionId={sessionId}
          onSelectSession={handleSelectSession}
          onNewChat={handleNewChat}
        />

        {/* Main chat area */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-4 py-6 flex flex-col">
            {messages.length === 0 && !streaming ? (
              /* Empty state - ChatGPT style */
              <div className="flex-1 flex flex-col items-center justify-center max-w-3xl mx-auto w-full">
                <div className="text-center mb-12">
                  <h2 className="text-3xl font-light text-gray-200 mb-3">
                    有什么可以帮忙的
                  </h2>
                  <p className="text-gray-500 text-sm">
                    动作指导 · 计划生成 · 知识问答 · 情感激励
                  </p>
                </div>
                <div className="w-full max-w-2xl">
                  <ChatInput onSend={handleSend} disabled={streaming} isEmptyState={true} />
                </div>
              </div>
            ) : (
              /* Chat messages */
              <div className="max-w-3xl mx-auto w-full flex-1">
                {messages.map((msg, i) => (
                  <ChatMessage key={i} message={msg} />
                ))}

                {streaming && (
                  <div className="mb-4">
                    <ThinkingStatus steps={thinkingSteps} />
                    {streamingContent && (
                      <ChatMessage message={{ role: 'assistant', content: streamingContent }} />
                    )}
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            )}

            {/* Input at bottom when chatting */}
            {(messages.length > 0 || streaming) && (
              <div className="max-w-3xl mx-auto w-full">
                <ChatInput onSend={handleSend} disabled={streaming} isEmptyState={false} />
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
