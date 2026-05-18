/**
 * SSE 流式连接封装
 * 对接后端 /api/fitness/workflow/execute/stream 接口
 */
export function connectWorkflowStream({ userInput, userId, sessionId, onMetadata, onToken, onDone, onError }) {
  const params = new URLSearchParams({ userInput })
  if (userId) params.set('userId', userId)
  if (sessionId) params.set('sessionId', sessionId)

  const url = `/api/api/fitness/workflow/execute/stream?${params.toString()}`
  const eventSource = new EventSource(url)

  eventSource.addEventListener('metadata', (e) => {
    try {
      const data = JSON.parse(e.data)
      onMetadata?.(data)
    } catch (err) {
      console.error('解析 metadata 事件失败', err)
    }
  })

  eventSource.addEventListener('token', (e) => {
    try {
      const data = JSON.parse(e.data)
      onToken?.(data)
    } catch (err) {
      console.error('解析 token 事件失败', err)
    }
  })

  eventSource.addEventListener('done', (e) => {
    try {
      const data = JSON.parse(e.data)
      onDone?.(data)
    } catch (err) {
      console.error('解析 done 事件失败', err)
    }
    eventSource.close()
  })

  eventSource.addEventListener('error', (e) => {
    if (e.data) {
      try {
        const data = JSON.parse(e.data)
        onError?.(data)
      } catch (err) {
        onError?.({ message: '连接错误' })
      }
    } else {
      onError?.({ message: '连接已断开' })
    }
    eventSource.close()
  })

  eventSource.onerror = () => {
    eventSource.close()
  }

  return eventSource
}
