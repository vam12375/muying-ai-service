interface Props {
  model: string;
  onChange: (model: string) => void;
}

const MODELS = [
  { value: 'deepseek', label: 'DeepSeek' },
  { value: 'qianwen', label: '通义千问' },
  { value: 'zhipu', label: '智谱清言' },
];

export function ModelSelector({ model, onChange }: Props) {
  return (
    <select
      value={model}
      onChange={e => onChange(e.target.value)}
      className="text-sm border border-gray-300 rounded-lg px-3 py-1.5 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-400 cursor-pointer"
    >
      {MODELS.map(m => (
        <option key={m.value} value={m.value}>
          {m.label}
        </option>
      ))}
    </select>
  );
}
