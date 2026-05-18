const NODE_LABELS = {
  workflow: '工作流',
  intent_recognition: '意图识别',
  user_profile: '用户画像',
  plan_generation: '计划生成',
  action_guidance: '动作指导',
  chat: '对话回复',
}

const STATUS_ICONS = {
  started: '⟳',
  routed: '✓',
  streaming: '⟳',
  completed: '✓',
  failed: '✗',
}

const STATUS_COLORS = {
  started: 'text-neutral-500',
  routed: 'text-neutral-700',
  streaming: 'text-neutral-500',
  completed: 'text-neutral-700',
  failed: 'text-red-500',
}

export default function ThinkingStatus({ steps }) {
  if (!steps || steps.length === 0) return null

  const isActive = steps.some(s => s.status === 'started' || s.status === 'streaming')

  return (
    <div className="bg-neutral-50 border border-neutral-200 rounded-lg p-3 mb-3 text-sm">
      <div className="flex items-center gap-2 mb-2">
        {isActive && (
          <span className="inline-block w-3 h-3 border-2 border-neutral-500 border-t-transparent rounded-full animate-spin-slow" />
        )}
        <span className="font-medium text-neutral-700">
          {isActive ? '正在思考...' : '思考完成'}
        </span>
      </div>
      <div className="space-y-1 ml-1">
        {steps.map((step, i) => (
          <div key={i} className="flex items-center gap-2">
            <span className={`text-xs ${STATUS_COLORS[step.status] || 'text-neutral-400'}`}>
              {STATUS_ICONS[step.status] || '○'}
            </span>
            <span className="text-neutral-600">
              {NODE_LABELS[step.node] || step.node}
            </span>
            {step.message && (
              <span className="text-neutral-400 text-xs">— {step.message}</span>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
