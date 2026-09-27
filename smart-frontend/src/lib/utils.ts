import { type ClassValue, clsx } from 'clsx'

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs)
}

export function formatCurrency(centavos: number): string {
  const reais = centavos / 100
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(reais)
}

export function formatDate(date: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(date))
}

export function formatDateTime(date: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date))
}

export function formatCpf(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  return digits.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
}

export function maskCpf(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  return digits
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}

export function isValidCpf(value: string): boolean {
  const cpf = value.replace(/\D/g, '')
  if (cpf.length !== 11) return false
  if (/^(\d)\1{10}$/.test(cpf)) return false
  const calcDigit = (length: number) => {
    let sum = 0
    for (let i = 0; i < length; i++) {
      sum += Number(cpf[i]) * (length + 1 - i)
    }
    const rest = (sum * 10) % 11
    return rest === 10 ? 0 : rest
  }
  return calcDigit(9) === Number(cpf[9]) && calcDigit(10) === Number(cpf[10])
}

export function formatCnpj(value: string): string {
  const chars = value.replace(/[^A-Za-z0-9]/g, '').slice(0, 14)
  return chars.replace(/(\w{2})(\w{3})(\w{3})(\w{4})(\w{2})/, '$1.$2.$3/$4-$5')
}

export function maskCnpj(value: string): string {
  const chars = value.replace(/[^A-Za-z0-9]/g, '').slice(0, 14)
  return chars
    .replace(/(\w{2})(\w)/, '$1.$2')
    .replace(/(\w{3})(\w)/, '$1.$2')
    .replace(/(\w{3})(\w)/, '$1/$2')
    .replace(/(\w{4})(\w)/, '$1-$2')
}

export function maskPhone(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length <= 10) {
    return digits
      .replace(/(\d{2})(\d)/, '($1) $2')
      .replace(/(\d{4})(\d)/, '$1-$2')
  }
  return digits
    .replace(/(\d{2})(\d)/, '($1) $2')
    .replace(/(\d{5})(\d)/, '$1-$2')
}

export function isValidPhone(value: string): boolean {
  const digits = value.replace(/\D/g, '')
  if (digits.length !== 10 && digits.length !== 11) return false
  const ddd = Number(digits.slice(0, 2))
  if (ddd < 11) return false
  if (digits.length === 11) return digits[2] === '9'
  return Number(digits[2]) >= 2 && Number(digits[2]) <= 5
}

export function maskCep(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 8)
  return digits.replace(/(\d{5})(\d)/, '$1-$2')
}

export function maskBarcode(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 13)
  if (digits.length <= 12) return digits
  return `${digits.slice(0, 12)}-${digits.slice(12)}`
}

export function maskCurrency(value: string): string {
  const digits = value.replace(/\D/g, '')
  if (!digits) return ''
  const num = Number(digits)
  const reais = (num / 100).toFixed(2)
  return reais.replace('.', ',')
}

export function formatPhone(value: string): string {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length <= 10) {
    return digits.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3')
  }
  return digits.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3')
}
