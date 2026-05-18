import { useState, useEffect } from 'react'

export default function ChatSidebar({ userId, activeSessionId, onSelectSession, onNewChat }) {
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(false)

  const fetchSessions = async () => {
    if (!userId) return
    setLoading(true)
    try {
      const res = await fetch(`/api/chat/sessions?userId=${userId}`)
      const data = await res.json()
      if (data.success) {
        setSessions(data.data || [])
      }
    } catch (err) {
      console.error('获取会话列表失败', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchSessions()
  }, [userId])

  // 暴露刷新方法给父组件
  useEffect(() => {
    if (window.__refreshSessions) return
    window.__refreshSessions = fetchSessions
    return () => { delete window.__refreshSessions }
  }, [userId])

  return (
    <div className="w-60 flex flex-col h-full border-r border-neutral-200 bg-neutral-50">
      <div className="p-3">
        <button
          onClick={onNewChat}
          className="w-full py-2.5 px-4 rounded-lg border border-neutral-300
                     hover:bg-white text-sm transition-all
                     text-left text-neutral-700 hover:text-neutral-900
                     flex items-center gap-2"
        >
          <span className="text-base">+</span>
          <span>新建会话</span>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {loading && sessions.length === 0 && (
          <p className="text-neutral-500 text-xs p-4">加载中...</p>
        )}
        {sessions.map((session) => (
          <div
            key={session.sessionId}
            onClick={() => onSelectSession(session.sessionId)}
            className={`px-4 py-2.5 cursor-pointer text-sm truncate transition-all rounded-lg mx-2 mb-0.5
              ${activeSessionId === session.sessionId
                ? 'bg-white text-neutral-900'
                : 'text-neutral-600 hover:bg-white/60 hover:text-neutral-900'}`}
            title={session.title}
          >
            {session.title}
          </div>
        ))}
      </div>
    </div>
  )
}
